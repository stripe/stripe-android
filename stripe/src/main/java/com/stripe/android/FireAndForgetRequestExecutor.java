package com.stripe.android;

import android.support.annotation.NonNull;

interface FireAndForgetRequestExecutor {
    /**
     * Execute the fire-and-forget request asynchronously.
     */
    void executeAsync(@NonNull StripeRequest request);
}
