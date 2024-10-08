package com.stripe.android.stripe3ds2.observability

import android.content.Context
import android.os.Build
import androidx.annotation.VisibleForTesting
import com.stripe.android.stripe3ds2.BuildConfig
import com.stripe.android.stripe3ds2.SdkVersion.VERSION_CODE
import com.stripe.android.stripe3ds2.SdkVersion.VERSION_NAME
import com.stripe.android.stripe3ds2.transaction.Logger
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
import javax.net.ssl.HttpsURLConnection
import kotlin.coroutines.CoroutineContext

/**
 * An [ErrorReporter] that reports to Sentry.
 */
internal class DefaultErrorReporter(
    private val context: Context,
    private val config: Config = EmptyConfig,
    private val workContext: CoroutineContext = Dispatchers.IO,
    private val logger: Logger = Logger.Noop,
    private val sentryConfig: SentryConfig,
    private val environment: String = BuildConfig.BUILD_TYPE,
    private val localeCountry: String = Locale.getDefault().country,
    private val osVersion: Int = Build.VERSION.SDK_INT
) : ErrorReporter {

    override fun reportError(t: Throwable) {
        CoroutineScope(workContext).launch {
            runCatching {
                send(
                    createRequestBody(t)
                )
            }.onFailure(::onFailure)
        }
    }

    private fun send(requestBody: JSONObject) {
        createPostConnection().let { connection ->
            connection.outputStream.use { os ->
                os.writer(StandardCharsets.UTF_8).use { osw ->
                    osw.write(requestBody.toString())
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

    private fun getResponseBody(
        responseStream: InputStream
    ) = runCatching {
        // \A is the beginning of the stream boundary
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
                HEADER_USER_AGENT to USER_AGENT,
                HEADER_SENTRY_AUTH to createSentryAuthHeader()
            ).forEach { (key, value) ->
                setRequestProperty(key, value)
            }
        }
    }

    private fun openConnection(): HttpsURLConnection {
        return URL("$HOST/api/${sentryConfig.projectId}/store/").openConnection() as HttpsURLConnection
    }

    @JvmSynthetic
    @VisibleForTesting
    internal fun createRequestBody(
        t: Throwable
    ): JSONObject {
        return JSONObject()
            .put("release", "${BuildConfig.LIBRARY_PACKAGE_NAME}@$VERSION_NAME+$VERSION_CODE")
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

    @JvmSynthetic
    @VisibleForTesting
    internal fun createRequestContexts(): JSONObject {
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

    @JvmSynthetic
    @VisibleForTesting
    internal fun createRequestStacktrace(t: Throwable): JSONObject {
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
        return listOf(
            "Sentry",
            listOf(
                "sentry_key" to sentryConfig.key,
                "sentry_version" to sentryConfig.version,
                "sentry_timestamp" to sentryConfig.getTimestamp(),
                "sentry_client" to USER_AGENT,
                "sentry_secret" to sentryConfig.secret
            ).joinToString(separator = ", ") { (key, value) ->
                "$key=$value"
            }
        ).joinToString(separator = " ")
    }

    private fun onFailure(exception: Throwable) {
        logger.error("Failed to send error report.", exception)
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
        private const val CONTENT_TYPE = "application/json; charset=utf-8"

        private const val HEADER_USER_AGENT = "User-Agent"
        private const val USER_AGENT = "Android3ds2Sdk $VERSION_NAME"

        private const val HEADER_SENTRY_AUTH = "X-Sentry-Auth"

        private val CHARSET = StandardCharsets.UTF_8.name()
    }
}
