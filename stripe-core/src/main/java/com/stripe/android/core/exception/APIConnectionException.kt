package com.stripe.android.core.exception

import androidx.annotation.RestrictTo
import java.io.IOException
import java.net.URLDecoder

/**
 * An [Exception] that represents a failure to connect to Stripe's API.
 */
class APIConnectionException(
    message: String? = null,
    cause: Throwable? = null
) : StripeException(
    cause = cause,
    message = message
) {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun analyticsValue(): String = "connectionError"

    companion object {
        private val SENSITIVE_PARAM_NAMES = setOf(
            "key",
            "client_secret",
            "ephemeral_key",
            "legacy_customer_ephemeral_key",
        )

        private val SENSITIVE_VALUE_PREFIXES = listOf(
            "ek_live_", "ek_test_",
            "pk_live_", "pk_test_",
            "sk_live_", "sk_test_",
            "uk_live_", "uk_test_",
            "rk_live_", "rk_test_",
        )

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @JvmSynthetic
        fun create(e: IOException, url: String? = null): APIConnectionException {
            val sanitizedUrl = url?.let(Companion::redactUrl)
            val displayUrl = listOfNotNull(
                "Stripe",
                "($sanitizedUrl)".takeUnless { sanitizedUrl.isNullOrBlank() }
            ).joinToString(" ")
            return APIConnectionException(
                "IOException during API request to $displayUrl: ${e.message}. " +
                    "Please check your internet connection and try again. " +
                    "If this problem persists, you should check Stripe's " +
                    "service status at https://status.stripe.com/, " +
                    "or let us know at support@stripe.com.",
                e
            )
        }

        private fun redactUrl(url: String): String {
            val queryIndex = url.indexOf('?')
            if (queryIndex < 0 || queryIndex == url.lastIndex) return url

            val baseUrl = url.substring(0, queryIndex)
            val queryString = url.substring(queryIndex + 1)

            val redactedParams = queryString.split("&").joinToString("&") { param ->
                val eqIndex = param.indexOf('=')
                if (eqIndex < 0) return@joinToString param
                val name = decodeComponent(param.substring(0, eqIndex))
                val value = decodeComponent(param.substring(eqIndex + 1))
                val redacted = if (shouldRedactParam(name, value)) "**REDACTED**" else value
                "$name=$redacted"
            }

            return "$baseUrl?$redactedParams"
        }

        private fun decodeComponent(encoded: String): String {
            return try {
                URLDecoder.decode(encoded, "UTF-8")
            } catch (_: IllegalArgumentException) {
                encoded
            }
        }

        private fun shouldRedactParam(name: String, value: String): Boolean {
            if (name in SENSITIVE_PARAM_NAMES) return true
            return SENSITIVE_VALUE_PREFIXES.any { value.startsWith(it) }
        }
    }
}
