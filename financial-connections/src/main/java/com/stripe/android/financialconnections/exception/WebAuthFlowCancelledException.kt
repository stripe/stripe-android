package com.stripe.android.financialconnections.exception

/**
 * Web auth flow was cancelled before completion.
 */
internal class WebAuthFlowCancelledException : Exception()

/**
 * Something went wrong while on the Web auth flow.
 *
 * @param message: exception message.
 * @param reason: reason received on return_url as query param.
 */
internal class WebAuthFlowFailedException(
    val reason: String?,
    message: String?
) : Exception("$message $reason")
