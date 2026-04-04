package com.stripe.android.core.networking

import androidx.annotation.RestrictTo
import com.stripe.android.core.ApiKeyValidator
import com.stripe.android.core.ApiVersion
import com.stripe.android.core.AppInfo
import com.stripe.android.core.model.CommonParcelable
import com.stripe.android.core.model.CommonParcelize
import com.stripe.android.core.version.StripeSdkVersion
import okio.BufferedSink

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
    private val headersFactory = RequestHeadersFactory.Api(
        options = options,
        appInfo = appInfo,
        apiVersion = apiVersion,
        sdkVersion = sdkVersion
    )

    private val postBodyBytes: ByteArray
        get() = query.toByteArray(Charsets.UTF_8)

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

    override fun writePostBody(sink: BufferedSink) {
        postBodyBytes.let {
            sink.write(it)
            sink.flush()
        }
    }

    override fun toString(): String {
        return "${method.code} $baseUrl"
    }

    /**
     * Data class representing options for a Stripe API request.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @CommonParcelize
    data class Options constructor(
        val apiKey: String,
        val stripeAccount: String? = null,
        val idempotencyKey: String? = null
    ) : CommonParcelable {

        val apiKeyIsUserKey: Boolean
            get() = apiKey.startsWith("uk_")

        val apiKeyIsLiveMode: Boolean
            get() = !apiKey.contains("test")

        /**
         * Dedicated constructor for injection.
         *
         * Because publishable key and stripe account providers might change, whenever required, a new
         * [ApiRequest.Options] instance is created with the latest values.
         * Should be used by DI code paths that need a fresh value on each request.
         */
        constructor(
            publishableKeyProvider: () -> String,
            stripeAccountIdProvider: () -> String?
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
        @Volatile
        var API_HOST_OVERRIDE: String? = null

        val API_HOST: String
            get() = API_HOST_OVERRIDE ?: "https://api.stripe.com"
    }
}
