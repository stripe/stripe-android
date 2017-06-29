package com.stripe.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.stripe.android.model.Address;

public class PaymentConfiguration {

    private static PaymentConfiguration mInstance;

    private @NonNull String mPublishableKey;
    private @Address.RequiredAddressFields int mRequiredAddressFields;
    private boolean mShouldUseSources;

    private PaymentConfiguration(@NonNull String publishableKey) {
        mPublishableKey = publishableKey;
    }

    @NonNull
    public static PaymentConfiguration getInstance() {
        if (mInstance == null) {
            throw new RuntimeException(
                    "Attempted to get instance of PaymentConfiguration without initialization.");
        }
        return mInstance;
    }

    public static void init(@NonNull String publishableKey) {
        mInstance = new PaymentConfiguration(publishableKey);
        mInstance.mRequiredAddressFields = Address.RequiredAddressFields.NONE;
        mInstance.mShouldUseSources = true;
    }

    @NonNull
    public String getPublishableKey() {
        return mPublishableKey;
    }

    public @Address.RequiredAddressFields int getRequiredAddressFields() {
        return mRequiredAddressFields;
    }

    @NonNull
    public PaymentConfiguration setRequiredAddressFields(
            @Address.RequiredAddressFields int requiredAddressFields) {
        mRequiredAddressFields = requiredAddressFields;
        return this;
    }

    public boolean getShouldUseSources() {
        return mShouldUseSources;
    }

    @NonNull
    public PaymentConfiguration setShouldUseSources(boolean shouldUseSources) {
        mShouldUseSources = shouldUseSources;
        return this;
    }

    @VisibleForTesting
    static void setInstance(@Nullable PaymentConfiguration paymentConfiguration) {
        mInstance = paymentConfiguration;
    }
}
