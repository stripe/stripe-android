package com.stripe.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.stripe.android.model.Address;

import java.util.Currency;

public class PaymentConfiguration {

    private static PaymentConfiguration mInstance;

    private @Nullable ClassLoader mEphemeralKeyProviderClassLoader;
    private @NonNull String mPublishableKey;
    private @Address.RequiredBillingAddressFields
    int mRequiredBillingAddressFields;
    private boolean mShouldUseSourcesForCards; // deprecated- this value is not used.
    private Currency mCurrency;

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
        mInstance.mRequiredBillingAddressFields = Address.RequiredBillingAddressFields.NONE;
        mInstance.mShouldUseSourcesForCards = true;
    }

    @NonNull
    public String getPublishableKey() {
        return mPublishableKey;
    }

    public @Address.RequiredBillingAddressFields
    int getRequiredBillingAddressFields() {
        return mRequiredBillingAddressFields;
    }

    @NonNull
    public PaymentConfiguration setRequiredBillingAddressFields(
            @Address.RequiredBillingAddressFields int requiredBillingAddressFields) {
        mRequiredBillingAddressFields = requiredBillingAddressFields;
        return this;
    }

    @Deprecated
    public boolean getShouldUseSourcesForCards() {
        return mShouldUseSourcesForCards;
    }

    @Deprecated
    @NonNull
    public PaymentConfiguration setShouldUseSourcesForCards(boolean shouldUseSourcesForCards) {
        mShouldUseSourcesForCards = shouldUseSourcesForCards;
        return this;
    }

    @VisibleForTesting
    static void setInstance(@Nullable PaymentConfiguration paymentConfiguration) {
        mInstance = paymentConfiguration;
    }
}
