package com.stripe.android.networking

import com.stripe.android.ApiVersion
import com.stripe.android.AppInfo
import com.stripe.android.Stripe
import java.util.Locale

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
        systemPropertySupplier: (String) -> String = DEFAULT_SYSTEM_PROPERTY_SUPPLIER,
        private val apiVersion: String = ApiVersion.get().code,
        private val sdkVersion: String = Stripe.VERSION
    ) : RequestHeadersFactory() {
        private val stripeClientUserAgentHeaderFactory = StripeClientUserAgentHeaderFactory(
            systemPropertySupplier
        )

        private val languageTag: String?
            get() {
                return locale.toString().replace("_", "-")
                    .takeIf { it.isNotBlank() }
            }

        override val userAgent: String
            get() {
                return listOfNotNull(
                    getUserAgent(sdkVersion),
                    appInfo?.toUserAgent()
                ).joinToString(" ")
            }

        override val extraHeaders: Map<String, String>
            get() {
                return mapOf(
                    "Accept" to "application/json",
                    "Stripe-Version" to apiVersion,
                    "Authorization" to "Bearer ${options.apiKey}"
                ).plus(
                    stripeClientUserAgentHeaderFactory.create(appInfo)
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

        private companion object {
            private val DEFAULT_SYSTEM_PROPERTY_SUPPLIER = { name: String ->
                System.getProperty(name).orEmpty()
            }
        }
    }

    class Fingerprint(
        guid: String
    ) : RequestHeadersFactory() {
        override val extraHeaders = mapOf(HEADER_COOKIE to "m=$guid")

        override val userAgent = getUserAgent(Stripe.VERSION)

        private companion object {
            private const val HEADER_COOKIE = "Cookie"
        }
    }

    object Analytics : RequestHeadersFactory() {
        override val userAgent = getUserAgent(Stripe.VERSION)
        override val extraHeaders = emptyMap<String, String>()
    }

    internal companion object {
        internal fun getUserAgent(
            sdkVersion: String = Stripe.VERSION
        ) = "Stripe/v1 $sdkVersion"

        private const val HEADER_USER_AGENT = "User-Agent"
        private const val HEADER_ACCEPT_CHARSET = "Accept-Charset"

        private val CHARSET = Charsets.UTF_8.name()
    }
}
