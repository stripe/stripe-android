package com.stripe.android.stripe3ds2.observability

import java.io.Serializable

/**
 * An interface for sending error reports to Stripe.
 */
fun interface ErrorReporter : Serializable {
    fun reportError(t: Throwable)
}
