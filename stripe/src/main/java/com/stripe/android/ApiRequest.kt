package com.stripe.android

import android.os.Build
import android.os.Parcelable
import com.stripe.android.exception.InvalidRequestException
import java.io.UnsupportedEncodingException
import java.util.Locale
import kotlinx.android.parcel.Parcelize
import org.json.JSONObject

/**
 * A class representing a Stripe API or Analytics request.
 */
internal data class ApiRequest internal constructor(
    override val method: Method,
    override val baseUrl: String,
    override val params: Map<String, *>? = null,
    internal val options: Options,
    private val appInfo: AppInfo? = null,
    private val systemPropertySupplier: (String) -> String = DEFAULT_SYSTEM_PROPERTY_SUPPLIER
) : StripeRequest() {
    private val apiVersion: String = ApiVersion.get().code
    override val mimeType: MimeType = MimeType.Form

    private val languageTag: String?
        get() {
            return Locale.getDefault().toString().replace("_", "-")
                .takeIf { it.isNotBlank() }
        }

    override fun createHeaders(): Map<String, String> {
        return mapOf(
            "Accept" to "application/json",
            HEADER_STRIPE_CLIENT_USER_AGENT to stripeClientUserAgent,
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

    private val stripeClientUserAgent: String
        get() {
            return JSONObject(
                mapOf(
                    "os.name" to "android",
                    "os.version" to Build.VERSION.SDK_INT.toString(),
                    "bindings.version" to BuildConfig.VERSION_NAME,
                    "lang" to "Java",
                    "publisher" to "Stripe",
                    "java.version" to systemPropertySupplier("java.version"),
                    "http.agent" to systemPropertySupplier(PROP_USER_AGENT)
                ).plus(
                    appInfo?.createClientHeaders().orEmpty()
                )
            ).toString()
        }

    override val userAgent: String = listOfNotNull(
        DEFAULT_USER_AGENT, appInfo?.toUserAgent()
    ).joinToString(" ")

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

    internal companion object {
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

        private val DEFAULT_SYSTEM_PROPERTY_SUPPLIER = { name: String ->
            System.getProperty(name).orEmpty()
        }
    }
}
