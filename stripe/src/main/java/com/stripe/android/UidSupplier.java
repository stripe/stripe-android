package com.stripe.android;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.support.annotation.NonNull;

import com.stripe.android.utils.ObjectUtils;

final class UidSupplier implements Supplier<StripeUid> {
    @NonNull private final ContentResolver mContentResolver;

    UidSupplier(@NonNull Context context) {
        mContentResolver = context.getApplicationContext().getContentResolver();
    }

    @SuppressWarnings("HardwareIds")
    @NonNull
    public StripeUid get() {
        final String androidId =
                Settings.Secure.getString(mContentResolver, Settings.Secure.ANDROID_ID);
        return StripeUid.create(ObjectUtils.getOrDefault(androidId, ""));
    }
}
