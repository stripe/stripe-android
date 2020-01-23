package com.stripe.android

import android.os.Parcelable
import com.stripe.android.exception.InvalidRequestException
import java.io.UnsupportedEncodingException
import kotlinx.android.parcel.Parcelize

/**
 * A class representing a Stripe API or Analytics request.
 */
internal data class ApiRequest internal constructor(
    override val method: Method,
    override val baseUrl: String,
    override val params: Map<String, *>? = null,
    internal val options: Options,
    private val appInfo: AppInfo? = null,
    private val systemPropertySupplier: (String) -> String = DEFAULT_SYSTEM_PROPERTY_SUPPLIER,
    private val apiVersion: String = ApiVersion.get().code,
    private val sdkVersion: String = Stripe.VERSION
) : StripeRequest() {
    override val mimeType: MimeType = MimeType.Form

    override val headersFactory = RequestHeadersFactory.Api(
        options = options,
        appInfo = appInfo,
        systemPropertySupplier = systemPropertySupplier,
        apiVersion = apiVersion,
        sdkVersion = sdkVersion
    )

    override val body: String
        @Throws(UnsupportedEncodingException::class, InvalidRequestException::class)
        get() {
            return query
        }

    override fun toString(): String {
        return "${method.code} $baseUrl"
    }

    /**
     * Data class representing options for a Stripe API request.
     */
    @Parcelize
    internal data class Options internal constructor(
        val apiKey: String,
        internal val stripeAccount: String? = null,
        internal val idempotencyKey: String? = null
    ) : Parcelable {
        init {
            ApiKeyValidator().requireValid(apiKey)
        }
    }

    class Factory(
        private val appInfo: AppInfo? = null,
        private val apiVersion: String = ApiVersion.get().code,
        private val sdkVersion: String = Stripe.VERSION
    ) {
        fun createGet(
            url: String,
            options: Options,
            params: Map<String, *>? = null
        ): ApiRequest {
            return ApiRequest(
                Method.GET, url, params, options, appInfo,
                apiVersion = apiVersion,
                sdkVersion = sdkVersion
            )
        }

        fun createPost(
            url: String,
            options: Options,
            params: Map<String, *>? = null
        ): ApiRequest {
            return ApiRequest(
                Method.POST, url, params, options, appInfo,
                apiVersion = apiVersion,
                sdkVersion = sdkVersion
            )
        }

        fun createDelete(
            url: String,
            options: Options
        ): ApiRequest {
            return ApiRequest(
                Method.DELETE,
                url,
                options = options,
                appInfo = appInfo,
                apiVersion = apiVersion,
                sdkVersion = sdkVersion
            )
        }
    }

    internal companion object {
        internal const val API_HOST = "https://api.stripe.com"

        internal const val HEADER_STRIPE_CLIENT_USER_AGENT = "X-Stripe-Client-User-Agent"
    }
}
