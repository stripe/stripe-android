package com.stripe.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

public class PaymentConfiguration {

    @Nullable private static PaymentConfiguration mInstance;
    @NonNull private final String mPublishableKey;

    private PaymentConfiguration(@NonNull String publishableKey) {
        mPublishableKey = publishableKey;
    }

    @NonNull
    public static PaymentConfiguration getInstance() {
        if (mInstance == null) {
            throw new IllegalStateException(
                    "Attempted to get instance of PaymentConfiguration without initialization.");
        }
        return mInstance;
    }

    public static void init(@NonNull String publishableKey) {
        mInstance = new PaymentConfiguration(publishableKey);
    }

    @NonNull
    public String getPublishableKey() {
        return mPublishableKey;
    }

    @VisibleForTesting
    static void clearInstance() {
        mInstance = null;
    }
}
