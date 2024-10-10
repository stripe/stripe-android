package com.stripe.android.core.networking

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.core.ApiKeyValidator
import com.stripe.android.core.ApiVersion
import com.stripe.android.core.AppInfo
import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.core.version.StripeSdkVersion
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
    val baseUrl: String,
    val params: Map<String, *>? = null,
    val options: Options,
    private val appInfo: AppInfo? = null,
    private val apiVersion: String = ApiVersion.get().code,
    private val sdkVersion: String = StripeSdkVersion.VERSION,
    override val shouldCache: Boolean = false
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

    override val retryResponseCodes: Iterable<Int> = DEFAULT_RETRY_CODES

    /**
     * If the HTTP method is [StripeRequest.Method.GET] or [StripeRequest.Method.DELETE], this is
     * the URL with query string; otherwise, just the URL.
     */
    override val url: String
        get() = if (Method.GET == method || Method.DELETE == method) {
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
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class Options constructor(
        val apiKey: String,
        val stripeAccount: String? = null,
        val idempotencyKey: String? = null
    ) : Parcelable {

        val apiKeyIsUserKey: Boolean
            get() = apiKey.startsWith("uk_")

        val apiKeyIsLiveMode: Boolean
            get() = !apiKey.contains("test")

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

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        companion object {
            const val UNDEFINED_PUBLISHABLE_KEY = "pk_undefined"
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Factory(
        private val appInfo: AppInfo? = null,
        private val apiVersion: String = ApiVersion.get().code,
        private val sdkVersion: String = StripeSdkVersion.VERSION
    ) {
        fun createGet(
            url: String,
            options: Options,
            params: Map<String, *>? = null,
            shouldCache: Boolean = false,
        ): ApiRequest {
            return ApiRequest(
                method = Method.GET,
                baseUrl = url,
                params = params,
                options = options,
                appInfo = appInfo,
                apiVersion = apiVersion,
                sdkVersion = sdkVersion,
                shouldCache = shouldCache
            )
        }

        fun createPost(
            url: String,
            options: Options,
            params: Map<String, *>? = null,
            shouldCache: Boolean = false,
        ): ApiRequest {
            return ApiRequest(
                method = Method.POST,
                baseUrl = url,
                params = params,
                options = options,
                appInfo = appInfo,
                apiVersion = apiVersion,
                sdkVersion = sdkVersion,
                shouldCache = shouldCache
            )
        }

        fun createDelete(
            url: String,
            options: Options,
            params: Map<String, *>? = null
        ): ApiRequest {
            return ApiRequest(
                Method.DELETE,
                url,
                params = params,
                options = options,
                appInfo = appInfo,
                apiVersion = apiVersion,
                sdkVersion = sdkVersion
            )
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        const val API_HOST = "https://api.stripe.com"
    }
}
