package com.stripe.android.core.networking

import androidx.annotation.RestrictTo
import com.stripe.android.core.ApiVersion
import com.stripe.android.core.AppInfo
import com.stripe.android.core.version.StripeSdkVersion
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

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

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    open class BaseApiHeadersFactory(
        private val optionsProvider: () -> ApiRequest.Options,
        private val appInfo: AppInfo? = null,
        private val languageTag: String? = defaultRequestHeadersLanguageTag(),
        private val apiVersion: String = ApiVersion.get().code,
        private val sdkVersion: String = StripeSdkVersion.VERSION
    ) : RequestHeadersFactory() {
        private val platformData = defaultRequestHeadersPlatformData()
        private val stripeClientUserAgentHeaderFactory = StripeClientUserAgentHeaderFactory()

        override val userAgent: String
            get() {
                return listOfNotNull(
                    getUserAgent(sdkVersion),
                    appInfo?.toUserAgent()
                ).joinToString(" ")
            }

        override val xStripeUserAgent: String
            get() {
                val paramMap = defaultXStripeUserAgentMap(platformData)
                appInfo?.let {
                    paramMap.putAll(it.toParamMap())
                }
                return paramMap.toJsonString()
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
                        mapOf(HEADER_STRIPE_LIVEMODE to platformData.isStripeLiveMode.toString())
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
                    normalizedLanguageTag(languageTag)?.let { mapOf(HEADER_ACCEPT_LANGUAGE to it) }.orEmpty()
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
        languageTag: String? = defaultRequestHeadersLanguageTag(),
        apiVersion: String = ApiVersion.get().code,
        sdkVersion: String = StripeSdkVersion.VERSION
    ) : BaseApiHeadersFactory(
        optionsProvider = { options },
        appInfo = appInfo,
        languageTag = languageTag,
        apiVersion = apiVersion,
        sdkVersion = sdkVersion
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
        languageTag: String? = defaultRequestHeadersLanguageTag(),
        apiVersion: String = ApiVersion.get().code,
        sdkVersion: String = StripeSdkVersion.VERSION,
        boundary: String
    ) : BaseApiHeadersFactory(
        optionsProvider = { options },
        appInfo = appInfo,
        languageTag = languageTag,
        apiVersion = apiVersion,
        sdkVersion = sdkVersion
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
        private val platformData = defaultRequestHeadersPlatformData()
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
                return defaultXStripeUserAgentMap(platformData).toJsonString()
            }
    }

    /**
     * Factory for [AnalyticsRequest].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data object Analytics : RequestHeadersFactory() {
        override val userAgent = getUserAgent(StripeSdkVersion.VERSION)
        override val extraHeaders = emptyMap<String, String>()
        override val xStripeUserAgent: String
            get() {
                return defaultXStripeUserAgentMap(defaultRequestHeadersPlatformData()).toJsonString()
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

private fun defaultXStripeUserAgentMap(
    platformData: RequestHeadersPlatformData
) = mutableMapOf<String, String?>(
    RequestHeadersFactory.LANG to RequestHeadersFactory.KOTLIN,
    AnalyticsFields.BINDINGS_VERSION to StripeSdkVersion.VERSION_NAME,
    AnalyticsFields.OS_VERSION to platformData.osVersion,
    RequestHeadersFactory.TYPE to platformData.deviceType,
    RequestHeadersFactory.MODEL to platformData.deviceModel
)

private fun normalizedLanguageTag(languageTag: String?): String? {
    return languageTag?.takeIf { it.isNotBlank() && it != RequestHeadersFactory.UNDETERMINED_LANGUAGE }
}

private fun Map<String, String?>.toJsonString(): String {
    return JsonObject(
        mapValues { (_, value) ->
            JsonPrimitive(value ?: "null")
        }
    ).toString()
}
