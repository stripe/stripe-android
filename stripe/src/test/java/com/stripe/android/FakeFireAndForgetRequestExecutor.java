package com.stripe.android;

import android.support.annotation.NonNull;

final class FakeFireAndForgetRequestExecutor implements FireAndForgetRequestExecutor {
    @Override
    public void executeAsync(@NonNull StripeRequest request) {
    }
}
