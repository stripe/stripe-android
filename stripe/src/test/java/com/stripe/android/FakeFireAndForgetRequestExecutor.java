package com.stripe.android;

import androidx.annotation.NonNull;

final class FakeFireAndForgetRequestExecutor implements FireAndForgetRequestExecutor {
    @Override
    public void executeAsync(@NonNull StripeRequest request) {
    }
}
