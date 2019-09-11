package com.stripe.android

internal class FakeFireAndForgetRequestExecutor : FireAndForgetRequestExecutor {
    override fun executeAsync(request: StripeRequest) {}
}
