package com.stripe.android.core.error

/**
 * An interface for sending error reports to Stripe.
 */
fun interface ErrorReporter {
    fun reportError(t: Throwable)
}