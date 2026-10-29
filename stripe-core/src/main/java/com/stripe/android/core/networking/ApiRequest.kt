package com.stripe.android.core.networking

import androidx.annotation.RestrictTo
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.ApiVersion
import com.stripe.android.core.AppInfo
import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.core.version.StripeSdkVersion
import java.io.OutputStream
import java.io.UnsupportedEncodingException

/**
 * A class representing a Stripe API or Analytics request.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class ApiRequest internal constructor(
    override val method: Method,
    val baseUrl: String,
    val params: Map<String, *>? = null,
    val options: RequestCredentials,
    val idempotencyKey: String?,
    private val appInfo: AppInfo? = null,
    private val apiVersion: String = ApiVersion.get().code,
    private val sdkVersion: String = StripeSdkVersion.VERSION
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
        sdkVersion = sdkVersion,
        idempotencyKey = idempotencyKey,
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

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Factory(
        private val appInfo: AppInfo? = null,
        private val apiVersion: String = ApiVersion.get().code,
        private val sdkVersion: String = StripeSdkVersion.VERSION
    ) {
        fun createGet(
            url: String,
            options: RequestCredentials?,
            params: Map<String, *>?,
            apiConfiguration: ApiConfiguration.State?,
            idempotencyKey: String?,
        ): ApiRequest {
            return ApiRequest(
                method = Method.GET,
                baseUrl = url,
                params = params,
                options = resolveRequestCredentials(apiConfiguration, options),
                appInfo = appInfo,
                apiVersion = ApiVersion.withBetas(apiVersion, apiConfiguration?.betas.orEmpty()),
                sdkVersion = sdkVersion,
                idempotencyKey = idempotencyKey,
            )
        }

        fun createPost(
            url: String,
            options: RequestCredentials?,
            params: Map<String, *>?,
            apiConfiguration: ApiConfiguration.State?,
            idempotencyKey: String?,
        ): ApiRequest {
            return ApiRequest(
                method = Method.POST,
                baseUrl = url,
                params = params,
                options = resolveRequestCredentials(apiConfiguration, options),
                appInfo = appInfo,
                apiVersion = ApiVersion.withBetas(apiVersion, apiConfiguration?.betas.orEmpty()),
                sdkVersion = sdkVersion,
                idempotencyKey = idempotencyKey,
            )
        }

        fun createDelete(
            url: String,
            options: RequestCredentials?,
            params: Map<String, *>?,
            apiConfiguration: ApiConfiguration.State?,
            idempotencyKey: String?,
        ): ApiRequest {
            return ApiRequest(
                method = Method.DELETE,
                baseUrl = url,
                params = params,
                options = resolveRequestCredentials(apiConfiguration, options),
                appInfo = appInfo,
                apiVersion = ApiVersion.withBetas(apiVersion, apiConfiguration?.betas.orEmpty()),
                sdkVersion = sdkVersion,
                idempotencyKey = idempotencyKey,
            )
        }

        fun createGet(
            url: String,
            options: RequestCredentials,
            params: Map<String, *>? = null,
            idempotencyKey: String?,
        ): ApiRequest {
            return ApiRequest(
                method = Method.GET,
                baseUrl = url,
                params = params,
                options = options,
                appInfo = appInfo,
                apiVersion = apiVersion,
                sdkVersion = sdkVersion,
                idempotencyKey = idempotencyKey,
            )
        }

        fun createPost(
            url: String,
            options: RequestCredentials,
            params: Map<String, *>? = null,
            idempotencyKey: String?,
        ): ApiRequest {
            return ApiRequest(
                method = Method.POST,
                baseUrl = url,
                params = params,
                options = options,
                appInfo = appInfo,
                apiVersion = apiVersion,
                sdkVersion = sdkVersion,
                idempotencyKey = idempotencyKey,
            )
        }

        fun createDelete(
            url: String,
            options: RequestCredentials,
            params: Map<String, *>? = null,
            idempotencyKey: String?,
        ): ApiRequest {
            return ApiRequest(
                Method.DELETE,
                url,
                params = params,
                options = options,
                appInfo = appInfo,
                apiVersion = apiVersion,
                sdkVersion = sdkVersion,
                idempotencyKey = idempotencyKey,
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
