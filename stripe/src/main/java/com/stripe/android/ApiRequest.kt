package com.stripe.android

import android.os.Build
import androidx.annotation.VisibleForTesting
import com.stripe.android.exception.InvalidRequestException
import java.io.UnsupportedEncodingException
import java.util.Locale
import java.util.Objects
import org.json.JSONObject

/**
 * A class representing a Stripe API or Analytics request.
 */
internal class ApiRequest internal constructor(
    method: Method,
    url: String,
    params: Map<String, *>? = null,
    internal val options: Options,
    private val appInfo: AppInfo? = null,
    private val systemPropertySupplier: SystemPropertySupplier = StripeSystemPropertySupplier()
) : StripeRequest(method, url, params, MIME_TYPE) {
    private val apiVersion: String = ApiVersion.get().code

    @VisibleForTesting
    internal val languageTag: String?
        get() {
            return Locale.getDefault().toString().replace("_", "-")
                .takeIf { it.isNotBlank() }
        }

    override fun createHeaders(): Map<String, String> {
        return mapOf(
            "Accept-Charset" to CHARSET,
            "Accept" to "application/json",
            HEADER_STRIPE_CLIENT_USER_AGENT to createStripeClientUserAgent(),
            "Stripe-Version" to apiVersion,
            "Authorization" to "Bearer ${options.apiKey}"
        ).plus(
            options.stripeAccount?.let {
                mapOf("Stripe-Account" to it)
            }.orEmpty()
        ).plus(
            options.idempotencyKey?.let {
                mapOf("Idempotency-Key" to it)
            }.orEmpty()
        ).plus(
            languageTag?.let { mapOf("Accept-Language" to it) }.orEmpty()
        )
    }

    private fun createStripeClientUserAgent(): String {
        return JSONObject(
            mapOf(
                "os.name" to "android",
                "os.version" to Build.VERSION.SDK_INT.toString(),
                "bindings.version" to BuildConfig.VERSION_NAME,
                "lang" to "Java",
                "publisher" to "Stripe",
                "java.version" to systemPropertySupplier.get("java.version"),
                "http.agent" to systemPropertySupplier.get(PROP_USER_AGENT)
            ).plus(
                appInfo?.createClientHeaders().orEmpty()
            )
        ).toString()
    }

    override fun getUserAgent(): String {
        return listOfNotNull(DEFAULT_USER_AGENT, appInfo?.toUserAgent())
            .joinToString(" ")
    }

    @Throws(UnsupportedEncodingException::class, InvalidRequestException::class)
    override fun getOutputBytes(): ByteArray {
        return query.toByteArray(charset(CHARSET))
    }

    override fun toString(): String {
        return "${method.code} $baseUrl"
    }

    override fun hashCode(): Int {
        return Objects.hash(baseHashCode, options, appInfo)
    }

    override fun equals(other: Any?): Boolean {
        return super.equals(other) || other is ApiRequest && typedEquals((other as ApiRequest?)!!)
    }

    private fun typedEquals(obj: ApiRequest): Boolean {
        return super.typedEquals(obj) &&
            options == obj.options &&
            appInfo == obj.appInfo
    }

    /**
     * Data class representing options for a Stripe API request.
     */
    internal data class Options internal constructor(
        val apiKey: String,
        internal val stripeAccount: String? = null,
        internal val idempotencyKey: String? = null
    ) {
        init {
            ApiKeyValidator().requireValid(apiKey)
        }
    }

    companion object {
        internal const val MIME_TYPE = "application/x-www-form-urlencoded"
        internal const val API_HOST = "https://api.stripe.com"

        internal const val HEADER_STRIPE_CLIENT_USER_AGENT = "X-Stripe-Client-User-Agent"

        // this is the default user agent set by the system
        private const val PROP_USER_AGENT = "http.agent"

        @JvmSynthetic
        internal fun createGet(
            url: String,
            options: Options,
            params: Map<String, *>? = null,
            appInfo: AppInfo? = null
        ): ApiRequest {
            return ApiRequest(Method.GET, url, params, options, appInfo)
        }

        @JvmSynthetic
        internal fun createPost(
            url: String,
            options: Options,
            params: Map<String, *>? = null,
            appInfo: AppInfo? = null
        ): ApiRequest {
            return ApiRequest(Method.POST, url, params, options, appInfo)
        }

        @JvmSynthetic
        internal fun createDelete(
            url: String,
            options: Options,
            appInfo: AppInfo? = null
        ): ApiRequest {
            return ApiRequest(Method.DELETE, url, null, options, appInfo)
        }
    }
}
