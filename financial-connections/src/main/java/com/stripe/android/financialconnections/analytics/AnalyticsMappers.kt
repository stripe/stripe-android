package com.stripe.android.financialconnections.analytics

import com.stripe.android.core.exception.StripeException
import com.stripe.android.financialconnections.exception.FinancialConnectionsError
import com.stripe.android.financialconnections.exception.WebAuthFlowFailedException

/**
 * Maps a [Throwable] instance into a [Map] of relevant failure details for analytics
 * purposes.
 */
internal fun Throwable.toEventParams(
    extraMessage: String?
): Map<String, String?> = when (this) {
    is WebAuthFlowFailedException -> mapOf(
        "error" to reason,
        "error_type" to reason,
        "error_stacktrace" to stackTraceToString(),
        "error_message" to listOfNotNull(
            message,
            extraMessage
        ).joinToString(" "),
        "code" to null
    )

    is FinancialConnectionsError -> mapOf(
        "error" to name,
        "error_type" to name,
        "error_stacktrace" to stackTraceToString(),
        "error_message" to listOfNotNull(
            stripeError?.message ?: message,
            extraMessage
        ).joinToString(" "),
        "code" to (stripeError?.code ?: statusCode.toString())
    )

    is StripeException -> mapOf(
        "error" to (stripeError?.type ?: this::class.java.simpleName),
        "error_type" to (stripeError?.type ?: this::class.java.simpleName),
        "error_stacktrace" to stackTraceToString(),
        "error_message" to listOfNotNull(
            (stripeError?.message ?: message)?.take(MAX_LOG_LENGTH),
            extraMessage
        ).joinToString(" "),
        "code" to (stripeError?.code ?: statusCode.toString())
    )

    else -> mapOf(
        "error" to this::class.java.simpleName,
        "error_type" to this::class.java.simpleName,
        "error_stacktrace" to stackTraceToString(),
        "error_message" to listOfNotNull(
            message?.take(MAX_LOG_LENGTH),
            extraMessage
        ).joinToString(" "),
        "code" to null
    )
}

private const val MAX_LOG_LENGTH = 100
