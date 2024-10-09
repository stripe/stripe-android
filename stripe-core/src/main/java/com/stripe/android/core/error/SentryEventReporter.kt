package com.stripe.android.core.error

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.annotation.VisibleForTesting
import com.stripe.android.core.BuildConfig
import com.stripe.android.core.version.StripeSdkVersion.VERSION_NAME
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Locale
import java.util.UUID.randomUUID
import java.util.concurrent.TimeUnit

/**
 * An [ErrorReporter] that reports to Sentry.
 */
class SentryEventReporter(
    context: Context,
    private val sentryConfig: SentryConfig,
    private val requestExecutor: SentryRequestExecutor,
    private val environment: String = BuildConfig.BUILD_TYPE,
    private val localeCountry: String = Locale.getDefault().country,
) : ErrorReporter {

    val deviceId by lazy { Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) }

    private val appContext = context.applicationContext

    override fun reportError(t: Throwable) {
        GlobalScope.launch(Dispatchers.IO) {
            val request = SentryEnvelopeRequest(
                projectId = sentryConfig.projectId,
                envelopeBody = createEnvelopeBody(t),
                headers = buildHeaders()
            )
            requestExecutor.sendErrorRequest(request)
        }
    }

    @VisibleForTesting
    internal fun createEnvelopeBody(t: Throwable): String {
        val eventId = generateEventId()
        val envelopeHeader = SentryEnvelopeHeader(eventId)
        val itemHeader = SentryItemHeader("event")
        val itemPayload = createEventPayload(t, eventId)

        return Json.encodeToString(envelopeHeader) + "\n" +
            Json.encodeToString(itemHeader) + "\n" +
            Json.encodeToString(itemPayload) + "\n"
    }

    private fun createEventPayload(
        t: Throwable,
        eventId: String
    ): SentryEvent {
        return SentryEvent(
            eventId = eventId,
            timestamp = System.currentTimeMillis() / 1000.0,
            platform = "android",
            release = VERSION_NAME,
            exception = SentryException(
                values = listOf(
                    SentryExceptionValue(
                        type = t::class.java.canonicalName.orEmpty(),
                        value = t.message.orEmpty(),
                        stacktrace = createRequestStacktrace(t)
                    )
                )
            ),
            tags = mapOf(
                "locale" to localeCountry,
                "environment" to environment,
                "android_os_version" to Build.VERSION.SDK_INT.toString()
            ),
            contexts = createRequestContexts(),
            user = SentryUser(
                id = deviceId
            )
        )
    }

    private fun createRequestStacktrace(t: Throwable): SentryStacktrace {
        return SentryStacktrace(
            frames = t.stackTrace.reversed().map { el ->
                SentryFrame(
                    lineno = el.lineNumber,
                    filename = el.className,
                    function = el.methodName
                )
            }
        )
    }

    private fun buildHeaders(): Map<String, String> = mapOf(
        HEADER_CONTENT_TYPE to CONTENT_TYPE,
        HEADER_SENTRY_AUTH to createSentryAuthHeader()
    )

    private fun createRequestContexts(): SentryContexts {
        val packageInfo = runCatching {
            appContext.packageManager.getPackageInfo(appContext.packageName, 0)
        }.getOrNull()

        val appName = packageInfo?.applicationInfo?.loadLabel(appContext.packageManager).toString()

        return SentryContexts(
            app = SentryAppContext(
                appIdentifier = appContext.packageName,
                appName = appName,
                appVersion = packageInfo?.versionName.orEmpty()
            ),
            os = SentryOsContext(
                name = "Android",
                version = Build.VERSION.RELEASE,
                type = Build.TYPE,
                build = Build.DISPLAY
            ),
            device = SentryDeviceContext(
                modelId = Build.ID,
                model = Build.MODEL,
                manufacturer = Build.MANUFACTURER,
                type = Build.TYPE,
                archs = Build.SUPPORTED_ABIS.toList()
            )
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

    private fun generateEventId(): String {
        return randomUUID().toString().replace("-", "")
    }

    /**
     * If [System.currentTimeMillis] returns `1600285647423`, this method will return
     * `"1600285647.423"`.
     */
    fun getTimestamp(): String {
        val timestamp = System.currentTimeMillis()
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timestamp)
        val fraction = timestamp - TimeUnit.SECONDS.toMillis(seconds)
        return "$seconds.$fraction"
    }

    private companion object {
        private const val HEADER_CONTENT_TYPE = "Content-Type"
        private const val CONTENT_TYPE = "application/x-sentry-envelope"
        private const val USER_AGENT = "Stripe/v1 android/$VERSION_NAME"

        private const val HEADER_SENTRY_AUTH = "X-Sentry-Auth"
    }
}
