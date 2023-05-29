package com.stripe.android.financialconnections.analytics

import com.stripe.android.core.exception.StripeException
import com.stripe.android.financialconnections.exception.FinancialConnectionsClientError
import com.stripe.android.financialconnections.exception.FinancialConnectionsStripeError
import com.stripe.android.financialconnections.exception.WebAuthFlowFailedException

/**
 * Maps a [Throwable] instance into a [Map] of relevant failure details for analytics
 * purposes.
 */
internal fun Throwable.toEventParams(): Map<String, String?> = when (this) {
    is WebAuthFlowFailedException -> mapOf(
        "error" to reason,
        "error_type" to reason,
        "error_message" to message,
        "code" to null
    )
    is FinancialConnectionsClientError -> mapOf(
        "error" to name,
        "error_type" to name,
        "error_message" to message,
    )
    is FinancialConnectionsStripeError -> mapOf(
        "error" to name,
        "error_type" to name,
        "error_message" to (stripeError?.message ?: message),
        "code" to (stripeError?.code ?: statusCode.toString())
    )

    is StripeException -> mapOf(
        "error" to (stripeError?.type ?: this::class.java.simpleName),
        "error_type" to (stripeError?.type ?: this::class.java.simpleName),
        "error_message" to (stripeError?.message ?: this.message)?.take(MAX_LOG_LENGTH),
        "code" to (stripeError?.code ?: this.statusCode.toString())
    )

    else -> mapOf(
        "error" to this::class.java.simpleName,
        "error_type" to this::class.java.simpleName,
        "error_message" to message?.take(MAX_LOG_LENGTH),
        "code" to null
    )
}

private const val MAX_LOG_LENGTH = 100
