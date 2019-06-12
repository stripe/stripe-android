package com.stripe.android;

import android.support.annotation.NonNull;

final class StripeUid {
    @NonNull final String value;

    @NonNull
    static StripeUid create(@NonNull String uid) {
        return new StripeUid(uid);
    }

    private StripeUid(@NonNull String value) {
        this.value = value;
    }
}
