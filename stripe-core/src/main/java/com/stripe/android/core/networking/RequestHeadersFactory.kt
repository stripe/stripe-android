package com.stripe.android.core.networking

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
                HEADER_ACCEPT_CHARSET to CHARSET
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

    open class BasePaymentApiHeadersFactory(
        private val options: ApiRequest.Options,
        private val appInfo: AppInfo? = null,
        private val locale: Locale = Locale.getDefault(),
        private val apiVersion: String = ApiVersion.get().code,
        private val sdkVersion: String = StripeSdkVersion.VERSION
    ) : RequestHeadersFactory() {
        private val stripeClientUserAgentHeaderFactory = StripeClientUserAgentHeaderFactory()

        private val languageTag: String?
            get() {
                return locale.toString().replace("_", "-")
                    .takeIf { it.isNotBlank() }
            }

        override val userAgent: String
            get() {
                return listOfNotNull(
                    getUserAgent(sdkVersion),
                    appInfo?.toUserAgent()
                ).joinToString(" ")
            }

        override val extraHeaders: Map<String, String>
            get() {
                return mapOf(
                    HEADER_ACCEPT to "application/json",
                    HEADER_STRIPE_VERSION to apiVersion,
                    HEADER_AUTHORIZATION to "Bearer ${options.apiKey}"
                ).plus(
                    stripeClientUserAgentHeaderFactory.create(appInfo)
                ).plus(
                    if (options.apiKeyIsUserKey) {
                        val isLiveMode = Os.getenv("Stripe-Livemode") != "false"
                        mapOf(HEADER_STRIPE_LIVEMODE to isLiveMode.toString())
                    } else {
                        emptyMap()
                    }
                ).plus(
                    options.stripeAccount?.let {
                        mapOf(HEADER_STRIPE_ACCOUNT to it)
                    }.orEmpty()
                ).plus(
                    options.idempotencyKey?.let {
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
    class Api(
        options: ApiRequest.Options,
        appInfo: AppInfo? = null,
        locale: Locale = Locale.getDefault(),
        apiVersion: String = ApiVersion.get().code,
        sdkVersion: String = StripeSdkVersion.VERSION,
    ) : BasePaymentApiHeadersFactory(
        options, appInfo, locale, apiVersion, sdkVersion
    ) {
        override var postHeaders = mapOf(
            HEADER_CONTENT_TYPE to "${StripeRequest.MimeType.Form}; charset=$CHARSET"
        )
    }

    /**
     * Factory for [FileUploadRequest].
     * TODO(ccen) Move FileUpload to payments-core.
     */
    class FileUpload(
        options: ApiRequest.Options,
        appInfo: AppInfo? = null,
        locale: Locale = Locale.getDefault(),
        apiVersion: String = ApiVersion.get().code,
        sdkVersion: String = StripeSdkVersion.VERSION,
        boundary: String
    ) : BasePaymentApiHeadersFactory(
        options, appInfo, locale, apiVersion, sdkVersion
    ) {
        override var postHeaders = mapOf(
            HEADER_CONTENT_TYPE to "${StripeRequest.MimeType.MultipartForm.code}; boundary=$boundary"
        )
    }

    /**
     * Factory for [FraudDetectionDataRequest].
     * TODO(ccen) Move FraudDetection to payments-core.
     */
    class FraudDetection(
        guid: String
    ) : RequestHeadersFactory() {
        override val extraHeaders = mapOf(HEADER_COOKIE to "m=$guid")

        override val userAgent = getUserAgent(StripeSdkVersion.VERSION)

        companion object {
            const val HEADER_COOKIE = "Cookie"
        }

        override var postHeaders = mapOf(
            HEADER_CONTENT_TYPE to "${StripeRequest.MimeType.Json}; charset=$CHARSET"
        )
    }

    /**
     * Factory for [AnalyticsRequest].
     */
    object Analytics : RequestHeadersFactory() {
        override val userAgent = getUserAgent(StripeSdkVersion.VERSION)
        override val extraHeaders = emptyMap<String, String>()
    }

    companion object {
        fun getUserAgent(
            sdkVersion: String = StripeSdkVersion.VERSION
        ) = "Stripe/v1 $sdkVersion"

        val CHARSET: String = Charsets.UTF_8.name()
    }
}
