package com.stripe.android

internal interface FireAndForgetRequestExecutor {
    /**
     * Execute the fire-and-forget request asynchronously.
     */
    fun executeAsync(request: StripeRequest)
}
