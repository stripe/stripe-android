package com.stripe.android;

import android.support.annotation.NonNull;

import java.util.UUID;

final class FakeUidSupplier implements Supplier<StripeUid> {
    @NonNull private final String mValue;

    FakeUidSupplier() {
        this(UUID.randomUUID().toString());
    }

    FakeUidSupplier(@NonNull String mValue) {
        this.mValue = mValue;
    }

    @NonNull
    @Override
    public StripeUid get() {
        return StripeUid.create(mValue);
    }
}
