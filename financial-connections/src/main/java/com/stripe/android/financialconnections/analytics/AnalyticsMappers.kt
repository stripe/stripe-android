package com.stripe.android.financialconnections.analytics

import com.stripe.android.core.exception.StripeException

internal fun Throwable.toEventParams(): Map<String, String?> = when (this) {
    is StripeException -> mapOf(
        "error_type" to (stripeError?.type ?: this::class.java.simpleName),
        "error_message" to (stripeError?.message ?: this.message)?.take(MAX_LOG_LENGTH),
        "code" to (stripeError?.code ?: this.statusCode.toString())
    )

    else -> mapOf(
        "error_type" to this::class.java.simpleName,
        "error_message" to message?.take(MAX_LOG_LENGTH),
        "code" to null
    )
}

private const val MAX_LOG_LENGTH = 100
