package com.stripe.android.stripe3ds2.observability

internal class FakeErrorReporter : ErrorReporter {
    override fun reportError(t: Throwable) {
    }
}
