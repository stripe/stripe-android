package com.stripe.android;

import android.support.annotation.NonNull;

final class FakeFireAndForgetRequestExecutor implements FireAndForgetRequestExecutor {
    @Override
    public int execute(@NonNull StripeRequest request) {
        return 200;
    }

    @Override
    public void executeAsync(@NonNull StripeRequest request) {
    }
}
