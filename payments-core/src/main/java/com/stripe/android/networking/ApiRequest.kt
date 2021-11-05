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
import java.io.OutputStream
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
    internal val baseUrl: String,
    internal val params: Map<String, *>? = null,
    internal val options: Options,
    private val appInfo: AppInfo? = null,
    private val apiVersion: String = ApiVersion.get().code,
    private val sdkVersion: String = Stripe.VERSION,
) : StripeRequest() {
    private val query: String = QueryStringFactory.createFromParamsWithEmptyValues(params)

    private val postBodyBytes: ByteArray
        @Throws(UnsupportedEncodingException::class, InvalidRequestException::class)
        get() {
            try {
                return query.toByteArray(Charsets.UTF_8)
            } catch (e: UnsupportedEncodingException) {
                throw InvalidRequestException(
                    message = "Unable to encode parameters to ${Charsets.UTF_8.name()}. " +
                        "Please contact support@stripe.com for assistance.",
                    cause = e
                )
            }
        }

    private val headersFactory = RequestHeadersFactory.Api(
        options = options,
        appInfo = appInfo,
        apiVersion = apiVersion,
        sdkVersion = sdkVersion
    )

    override val mimeType: MimeType = MimeType.Form

    override val retryResponseCodes: Iterable<Int> = PAYMENT_RETRY_CODES

    /**
     * If the HTTP method is [Method.GET], this is the URL with query string;
     * otherwise, just the URL.
     */
    override val url: String
        get() = if (Method.GET == method) {
            listOfNotNull(
                baseUrl,
                query.takeIf { it.isNotEmpty() }
            ).joinToString(
                // In some cases, URL can already contain a question mark
                // (eg, upcoming invoice lines)
                separator = if (baseUrl.contains("?")) {
                    "&"
                } else {
                    "?"
                }
            )
        } else {
            baseUrl
        }

    override val headers: Map<String, String> = headersFactory.create()

    override var postHeaders: Map<String, String>? = headersFactory.createPostHeader()

    override fun writePostBody(outputStream: OutputStream) {
        postBodyBytes.let {
            outputStream.write(it)
            outputStream.flush()
        }
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
                method = Method.GET,
                baseUrl = url,
                params = params,
                options = options,
                appInfo = appInfo,
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
                method = Method.POST,
                baseUrl = url,
                params = params,
                options = options,
                appInfo = appInfo,
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
