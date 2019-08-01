package com.stripe.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

public final class PaymentConfiguration {

    @Nullable private static PaymentConfiguration mInstance;
    @NonNull private final String mPublishableKey;

    private PaymentConfiguration(@NonNull String publishableKey) {
        mPublishableKey = ApiKeyValidator.get().requireValid(publishableKey);
    }

    @NonNull
    public static PaymentConfiguration getInstance() {
        if (mInstance == null) {
            throw new IllegalStateException(
                    "Attempted to get instance of PaymentConfiguration without initialization.");
        }
        return mInstance;
    }

    /**
     * A publishable key from the Dashboard's
     * <a href="https://dashboard.stripe.com/apikeys">API keys</a> page.
     */
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
