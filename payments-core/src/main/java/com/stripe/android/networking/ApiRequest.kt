package com.stripe.android.networking

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.ApiKeyValidator
import com.stripe.android.ApiVersion
import com.stripe.android.AppInfo
import com.stripe.android.Stripe
import com.stripe.android.exception.InvalidRequestException
import com.stripe.android.payments.core.injection.PUBLISHABLE_KEY
import com.stripe.android.payments.core.injection.STRIPE_ACCOUNT_ID
import kotlinx.parcelize.Parcelize
import java.io.UnsupportedEncodingException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider

/**
 * A class representing a Stripe API or Analytics request.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class ApiRequest internal constructor(
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
    data class Options constructor(
        internal val apiKey: String,
        internal val stripeAccount: String? = null,
        internal val idempotencyKey: String? = null
    ) : Parcelable {

        /**
         * Dedicated constructor for injection.
         *
         * Because [PUBLISHABLE_KEY] and [STRIPE_ACCOUNT_ID] might change, whenever required, a new
         * [ApiRequest.Options] instance is created with the latest values.
         * Should always be used with [Provider] or [Lazy].
         */
        @Inject
        constructor(
            @Named(PUBLISHABLE_KEY) publishableKeyProvider: () -> String,
            @Named(STRIPE_ACCOUNT_ID) stripeAccountIdProvider: () -> String?
        ) : this(
            apiKey = publishableKeyProvider(),
            stripeAccount = stripeAccountIdProvider()
        )

        init {
            ApiKeyValidator().requireValid(apiKey)
        }

        companion object {
            const val UNDEFINED_PUBLISHABLE_KEY = "pk_undefined"
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
                Method.GET,
                url,
                params,
                options,
                appInfo,
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
                Method.POST,
                url,
                params,
                options,
                appInfo,
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
    }
}
