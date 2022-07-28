package com.stripe.android.financialconnections.exception

/**
 * Web auth flow was cancelled before completion.
 */
internal class WebAuthFlowCancelledException : Exception()
/**
 * Something went wrong while on the Web auth flow.
 * TODO@carlosmuvi add better exception granularity to auth flow failures.
 */
internal class WebAuthFlowFailedException(val url: String?) : Exception()
