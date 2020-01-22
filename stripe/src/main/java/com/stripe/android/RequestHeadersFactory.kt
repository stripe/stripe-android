package com.stripe.android

import android.os.Build
import java.util.Locale
import org.json.JSONObject

internal sealed class RequestHeadersFactory {
    fun create(): Map<String, String> {
        return extraHeaders.plus(
            mapOf(
                HEADER_USER_AGENT to userAgent,
                HEADER_ACCEPT_CHARSET to CHARSET
            )
        )
    }

    protected abstract val userAgent: String

    protected abstract val extraHeaders: Map<String, String>

    class Api(
        private val options: ApiRequest.Options,
        private val appInfo: AppInfo? = null,
        private val locale: Locale = Locale.getDefault(),
        private val systemPropertySupplier: (String) -> String = DEFAULT_SYSTEM_PROPERTY_SUPPLIER,
        private val apiVersion: String = ApiVersion.get().code,
        private val sdkVersion: String = Stripe.VERSION
    ) : RequestHeadersFactory() {
        private val languageTag: String?
            get() {
                return locale.toString().replace("_", "-")
                    .takeIf { it.isNotBlank() }
            }

        override val userAgent: String
            get() {
                return listOfNotNull(
                    getUserAgent(sdkVersion), appInfo?.toUserAgent()
                ).joinToString(" ")
            }

        override val extraHeaders: Map<String, String>
            get() {
                return mapOf(
                    "Accept" to "application/json",
                    ApiRequest.HEADER_STRIPE_CLIENT_USER_AGENT to createStripeClientUserAgent(appInfo),
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

        private fun createStripeClientUserAgent(appInfo: AppInfo? = null): String {
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

        private companion object {
            // this is the default user agent set by the system
            private const val PROP_USER_AGENT = "http.agent"

            private val DEFAULT_SYSTEM_PROPERTY_SUPPLIER = { name: String ->
                System.getProperty(name).orEmpty()
            }
        }
    }

    class Default(
        override val extraHeaders: Map<String, String> = emptyMap(),
        sdkVersion: String = Stripe.VERSION
    ) : RequestHeadersFactory() {
        override val userAgent: String = getUserAgent(sdkVersion)
    }

    internal companion object {
        internal fun getUserAgent(sdkVersion: String = Stripe.VERSION) = "Stripe/v1 $sdkVersion"

        private const val HEADER_USER_AGENT = "User-Agent"
        private const val HEADER_ACCEPT_CHARSET = "Accept-Charset"

        private val CHARSET = Charsets.UTF_8.name()
    }
}
