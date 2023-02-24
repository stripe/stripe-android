package com.stripe.android.core.networking

import android.os.Build
import android.system.Os
import androidx.annotation.RestrictTo
import com.stripe.android.core.ApiVersion
import com.stripe.android.core.AppInfo
import com.stripe.android.core.version.StripeSdkVersion
import java.util.Locale

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class RequestHeadersFactory {
    /**
     * Creates a map for headers attached to all requests.
     */
    fun create(): Map<String, String> {
        return extraHeaders.plus(
            mapOf(
                HEADER_USER_AGENT to userAgent,
                HEADER_ACCEPT_CHARSET to CHARSET,
                HEADER_X_STRIPE_USER_AGENT to xStripeUserAgent
            )
        )
    }

    /**
     * Creates a map for headers attached to POST requests. Return am empty map if this factory is
     * used for request that doesn't have POST options.
     */
    fun createPostHeader(): Map<String, String> {
        return postHeaders
    }

    protected abstract val userAgent: String

    protected abstract val extraHeaders: Map<String, String>

    protected open var postHeaders: Map<String, String> = emptyMap()

    protected abstract val xStripeUserAgent: String

    protected fun defaultXStripeUserAgentMap() = mutableMapOf<String, String?>(
        LANG to KOTLIN,
        AnalyticsFields.BINDINGS_VERSION to StripeSdkVersion.VERSION_NAME,
        AnalyticsFields.OS_VERSION to "${Build.VERSION.SDK_INT}",
        TYPE to "${Build.MANUFACTURER}_${Build.BRAND}_${Build.MODEL}",
        MODEL to Build.MODEL
    )

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    open class BaseApiHeadersFactory(
        private val optionsProvider: () -> ApiRequest.Options,
        private val appInfo: AppInfo? = null,
        private val locale: Locale = Locale.getDefault(),
        private val apiVersion: String = ApiVersion.get().code,
        private val sdkVersion: String = StripeSdkVersion.VERSION
    ) : RequestHeadersFactory() {
        private val stripeClientUserAgentHeaderFactory = StripeClientUserAgentHeaderFactory()

        private val languageTag: String?
            get() {
                return locale.toLanguageTag()
                    .takeIf { it.isNotBlank() && it != UNDETERMINED_LANGUAGE }
            }

        override val userAgent: String
            get() {
                return listOfNotNull(
                    getUserAgent(sdkVersion),
                    appInfo?.toUserAgent()
                ).joinToString(" ")
            }

        override val xStripeUserAgent: String
            get() {
                val paramMap = defaultXStripeUserAgentMap()
                appInfo?.let {
                    paramMap.putAll(it.toParamMap())
                }

                return "{" + paramMap.map { (key, value) ->
                    "\"$key\":\"$value\""
                }.joinToString(",") + "}"
            }

        override val extraHeaders: Map<String, String>
            get() {
                val apiRequestOptions = optionsProvider()
                return mapOf(
                    HEADER_ACCEPT to "application/json",
                    HEADER_STRIPE_VERSION to apiVersion,
                    HEADER_AUTHORIZATION to "Bearer ${apiRequestOptions.apiKey}"
                ).plus(
                    stripeClientUserAgentHeaderFactory.create(appInfo)
                ).plus(
                    if (apiRequestOptions.apiKeyIsUserKey) {
                        val isLiveMode = Os.getenv("Stripe-Livemode") != "false"
                        mapOf(HEADER_STRIPE_LIVEMODE to isLiveMode.toString())
                    } else {
                        emptyMap()
                    }
                ).plus(
                    apiRequestOptions.stripeAccount?.let {
                        mapOf(HEADER_STRIPE_ACCOUNT to it)
                    }.orEmpty()
                ).plus(
                    apiRequestOptions.idempotencyKey?.let {
                        mapOf(HEADER_IDEMPOTENCY_KEY to it)
                    }.orEmpty()
                ).plus(
                    languageTag?.let { mapOf(HEADER_ACCEPT_LANGUAGE to it) }.orEmpty()
                )
            }
    }

    /**
     * Factory for [ApiRequest].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Api(
        options: ApiRequest.Options,
        appInfo: AppInfo? = null,
        locale: Locale = Locale.getDefault(),
        apiVersion: String = ApiVersion.get().code,
        sdkVersion: String = StripeSdkVersion.VERSION
    ) : BaseApiHeadersFactory(
        { options },
        appInfo,
        locale,
        apiVersion,
        sdkVersion
    ) {
        override var postHeaders = mapOf(
            HEADER_CONTENT_TYPE to "${StripeRequest.MimeType.Form.code}; charset=$CHARSET"
        )
    }

    /**
     * Factory for [FileUploadRequest].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class FileUpload(
        options: ApiRequest.Options,
        appInfo: AppInfo? = null,
        locale: Locale = Locale.getDefault(),
        apiVersion: String = ApiVersion.get().code,
        sdkVersion: String = StripeSdkVersion.VERSION,
        boundary: String
    ) : BaseApiHeadersFactory(
        { options },
        appInfo,
        locale,
        apiVersion,
        sdkVersion
    ) {
        override var postHeaders = mapOf(
            HEADER_CONTENT_TYPE to "${StripeRequest.MimeType.MultipartForm.code}; boundary=$boundary"
        )
    }

    /**
     * Factory for [FraudDetectionDataRequest].
     * TODO(ccen) Move FraudDetection to payments-core.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class FraudDetection(
        guid: String
    ) : RequestHeadersFactory() {
        override val extraHeaders = mapOf(HEADER_COOKIE to "m=$guid")

        override val userAgent = getUserAgent(StripeSdkVersion.VERSION)

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        companion object {
            const val HEADER_COOKIE = "Cookie"
        }

        override var postHeaders = mapOf(
            HEADER_CONTENT_TYPE to "${StripeRequest.MimeType.Json.code}; charset=$CHARSET"
        )
        override val xStripeUserAgent: String
            get() {
                val paramMap = defaultXStripeUserAgentMap()
                return "{" + paramMap.map { (key, value) ->
                    "\"$key\":\"$value\""
                }.joinToString(",") + "}"
            }
    }

    /**
     * Factory for [AnalyticsRequest].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    object Analytics : RequestHeadersFactory() {
        override val userAgent = getUserAgent(StripeSdkVersion.VERSION)
        override val extraHeaders = emptyMap<String, String>()
        override val xStripeUserAgent: String
            get() {
                val paramMap = defaultXStripeUserAgentMap()
                return "{" + paramMap.map { (key, value) ->
                    "\"$key\":\"$value\""
                }.joinToString(",") + "}"
            }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        fun getUserAgent(
            sdkVersion: String = StripeSdkVersion.VERSION
        ) = "Stripe/v1 $sdkVersion"

        val CHARSET: String = Charsets.UTF_8.name()

        const val UNDETERMINED_LANGUAGE = "und"

        const val LANG = "lang"
        const val KOTLIN = "kotlin"
        const val TYPE = "type"
        const val MODEL = "model"
    }
}
