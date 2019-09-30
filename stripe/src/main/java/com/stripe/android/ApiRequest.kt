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
    params: Map<String, *>?,
    internal val options: Options,
    private val appInfo: AppInfo?
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
            "X-Stripe-Client-User-Agent" to createStripeClientUserAgent(),
            "Stripe-Version" to apiVersion,
            "Authorization" to String.format(Locale.ENGLISH, "Bearer %s", options.apiKey)
        ).plus(
            options.stripeAccount?.let {
                mapOf("Stripe-Account" to it)
            } ?: emptyMap()
        ).plus(
            (languageTag.takeIf { SHOULD_INCLUDE_ACCEPT_LANGUAGE_HEADER })?.let {
                mapOf("Accept-Language" to it)
            } ?: emptyMap()
        )
    }

    private fun createStripeClientUserAgent(): String {
        val javaVersion = System.getProperty("java.version")
        val propertyMap = mapOf(
            "os.name" to "android",
            "os.version" to Build.VERSION.SDK_INT.toString(),
            "bindings.version" to BuildConfig.VERSION_NAME,
            "lang" to "Java",
            "publisher" to "Stripe"
        ).plus(
            appInfo?.createClientHeaders() ?: emptyMap()
        ).plus(
            if (javaVersion != null) {
                mapOf("java.version" to javaVersion)
            } else {
                emptyMap()
            }
        )

        return JSONObject(propertyMap).toString()
    }

    override fun getUserAgent(): String {
        return listOfNotNull(DEFAULT_USER_AGENT, appInfo?.toUserAgent())
            .joinToString(" ")
    }

    @Throws(UnsupportedEncodingException::class, InvalidRequestException::class)
    override fun getOutputBytes(): ByteArray {
        return createQuery().toByteArray(charset(CHARSET))
    }

    override fun toString(): String {
        return String.format(Locale.ROOT,
            "%s %s",
            method.code,
            baseUrl
        )
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
    internal class Options private constructor(
        apiKey: String,
        val stripeAccount: String?
    ) {
        val apiKey: String = ApiKeyValidator().requireValid(apiKey)

        override fun hashCode(): Int {
            return Objects.hash(apiKey, stripeAccount)
        }

        override fun equals(other: Any?): Boolean {
            return when {
                this === other -> true
                other is Options -> typedEquals(other)
                else -> false
            }
        }

        private fun typedEquals(obj: Options): Boolean {
            return apiKey == obj.apiKey && stripeAccount == obj.stripeAccount
        }

        companion object {
            @JvmStatic
            fun create(apiKey: String): Options {
                return Options(apiKey, null)
            }

            @JvmStatic
            fun create(apiKey: String, stripeAccount: String?): Options {
                return Options(apiKey, stripeAccount)
            }
        }
    }

    companion object {
        internal const val MIME_TYPE = "application/x-www-form-urlencoded"
        internal const val API_HOST = "https://api.stripe.com"

        // TODO(mshafrir-stripe) - enable in next major version
        private const val SHOULD_INCLUDE_ACCEPT_LANGUAGE_HEADER = false

        @JvmStatic
        fun createGet(
            url: String,
            options: Options,
            appInfo: AppInfo? = null
        ): ApiRequest {
            return ApiRequest(Method.GET, url, null, options, appInfo)
        }

        @JvmStatic
        fun createGet(
            url: String,
            params: Map<String, *>,
            options: Options,
            appInfo: AppInfo? = null
        ): ApiRequest {
            return ApiRequest(Method.GET, url, params, options, appInfo)
        }

        @JvmStatic
        fun createPost(
            url: String,
            options: Options,
            appInfo: AppInfo? = null
        ): ApiRequest {
            return ApiRequest(Method.POST, url, null, options, appInfo)
        }

        @JvmStatic
        fun createPost(
            url: String,
            params: Map<String, *>,
            options: Options,
            appInfo: AppInfo? = null
        ): ApiRequest {
            return ApiRequest(Method.POST, url, params, options, appInfo)
        }

        @JvmStatic
        fun createDelete(
            url: String,
            options: Options,
            appInfo: AppInfo? = null
        ): ApiRequest {
            return ApiRequest(Method.DELETE, url, null, options, appInfo)
        }
    }
}
