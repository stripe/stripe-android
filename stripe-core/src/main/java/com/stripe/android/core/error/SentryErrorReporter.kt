package com.stripe.android.core.error

import android.content.Context
import android.os.Build
import androidx.annotation.VisibleForTesting
import com.stripe.android.core.BuildConfig
import com.stripe.android.core.Logger
import com.stripe.android.core.version.StripeSdkVersion.VERSION_NAME
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.Scanner
import java.util.UUID.randomUUID
import javax.net.ssl.HttpsURLConnection
import kotlin.coroutines.CoroutineContext

/**
 * An [ErrorReporter] that reports to Sentry.
 */
class SentryErrorReporter(
    private val context: Context,
    private val config: Config = EmptyConfig,
    private val workContext: CoroutineContext = Dispatchers.IO,
    private val logger: Logger,
    private val sentryConfig: SentryConfig,
    private val environment: String = BuildConfig.BUILD_TYPE,
    private val localeCountry: String = Locale.getDefault().country,
    private val osVersion: Int = Build.VERSION.SDK_INT
) : ErrorReporter {

    override fun reportError(t: Throwable) {
        CoroutineScope(workContext).launch {
            runCatching {
                send(
                    createEnvelopeBody(t)
                )
            }.onFailure(::onFailure)
        }
    }

    private fun send(envelopeBody: String) {
        createPostConnection().let { connection ->
            connection.outputStream.use { os ->
                os.writer(StandardCharsets.UTF_8).use { osw ->
                    osw.write(envelopeBody)
                    osw.flush()
                }
            }

            connection.connect()

            connection.responseCode.also { responseCode ->
                logResponse(connection, responseCode)
            }

            connection.disconnect()
        }
    }

    private fun logResponse(connection: HttpsURLConnection, responseCode: Int) {
        if (BuildConfig.DEBUG) {
            if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }?.use {
                logger.error(getResponseBody(it))
            }
        }
    }

    private fun getResponseBody(responseStream: InputStream) = runCatching {
        val scanner = Scanner(responseStream, CHARSET).useDelimiter("\\A")
        if (scanner.hasNext()) {
            scanner.next()
        } else {
            null
        }
    }.getOrNull().orEmpty()

    private fun createPostConnection(): HttpsURLConnection {
        return openConnection().apply {
            requestMethod = HTTP_METHOD
            doOutput = true

            mapOf(
                HEADER_CONTENT_TYPE to CONTENT_TYPE,
                HEADER_SENTRY_AUTH to createSentryAuthHeader()
            ).forEach { (key, value) ->
                setRequestProperty(key, value)
            }
        }
    }

    private fun openConnection(): HttpsURLConnection {
        return URL("$HOST/api/${sentryConfig.projectId}/envelope/").openConnection() as HttpsURLConnection
    }

    @VisibleForTesting
    internal fun createEnvelopeBody(t: Throwable): String {
        val eventId = generateEventId()
        val header = JSONObject().put("event_id", eventId)
        val itemHeaders = JSONObject().put("type", "event")
        val itemPayload = createEventPayload(t, eventId)
        return "$header\n$itemHeaders\n$itemPayload\n"
    }

    private fun createEventPayload(t: Throwable, eventId: String): JSONObject {
        return JSONObject()
            .put("event_id", eventId)
            .put("timestamp", System.currentTimeMillis() / 1000.0)
            .put("platform", "android")
            .put("release", VERSION_NAME)
            .put(
                "exception",
                JSONObject()
                    .put(
                        "values",
                        JSONArray()
                            .put(
                                JSONObject()
                                    .put("type", t::class.java.canonicalName)
                                    .put("value", t.message.orEmpty())
                                    .put("stacktrace", createRequestStacktrace(t))
                            )
                    )
            )
            .put(
                "tags",
                JSONObject()
                    .put("locale", localeCountry)
                    .put("environment", environment)
                    .put("android_os_version", osVersion)
                    .also {
                        config.customTags.forEach { (key, value) ->
                            it.put(key, value)
                        }
                    }
            )
            .put("contexts", createRequestContexts())
    }

    private fun createRequestContexts(): JSONObject {
        val packageInfo = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0)
        }.getOrNull()

        val appName = packageInfo?.applicationInfo?.loadLabel(context.packageManager)
        return JSONObject()
            .put(
                "app",
                JSONObject()
                    .put("app_identifier", context.packageName)
                    .put("app_name", appName)
                    .put("app_version", packageInfo?.versionName.orEmpty())
            )
            .put(
                "os",
                JSONObject()
                    .put("name", "Android")
                    .put("version", Build.VERSION.RELEASE)
                    .put("type", Build.TYPE)
                    .put("build", Build.DISPLAY)
            )
            .put(
                "device",
                JSONObject()
                    .put("model_id", Build.ID)
                    .put("model", Build.MODEL)
                    .put("manufacturer", Build.MANUFACTURER)
                    .put("type", Build.TYPE)
                    .put(
                        "archs",
                        JSONArray().also { archs ->
                            Build.SUPPORTED_ABIS.forEach { arch -> archs.put(arch) }
                        }
                    )
            )
    }

    private fun createRequestStacktrace(t: Throwable): JSONObject {
        return JSONObject()
            .put(
                "frames",
                JSONArray().also { frames ->
                    t.stackTrace.reversed().forEach { el ->
                        frames.put(
                            JSONObject()
                                .put("lineno", el.lineNumber)
                                .put("filename", el.className)
                                .put("function", el.methodName)
                        )
                    }
                }
            )
    }

    @JvmSynthetic
    @VisibleForTesting
    internal fun createSentryAuthHeader(): String {
        return "Sentry " + listOf(
            "sentry_key" to sentryConfig.key,
            "sentry_version" to sentryConfig.version,
            "sentry_client" to USER_AGENT
        ).joinToString(", ") { (key, value) -> "$key=$value" }
    }

    private fun onFailure(exception: Throwable) {
        logger.error("Failed to send error report.", exception)
    }

    private fun generateEventId(): String {
        // Generate a hexadecimal string representing a uuid4 value
        return randomUUID().toString().replace("-", "")
    }

    interface Config {
        val customTags: Map<String, String>
    }

    internal object EmptyConfig : Config {
        override val customTags: Map<String, String> = emptyMap()
    }

    private companion object {
        private const val HOST = "https://errors.stripe.com"
        private const val HTTP_METHOD = "POST"

        private const val HEADER_CONTENT_TYPE = "Content-Type"
        private const val CONTENT_TYPE = "application/x-sentry-envelope"
        private const val USER_AGENT = "Stripe/v1 android/$VERSION_NAME"

        private const val HEADER_SENTRY_AUTH = "X-Sentry-Auth"

        private val CHARSET = StandardCharsets.UTF_8.name()
    }
}