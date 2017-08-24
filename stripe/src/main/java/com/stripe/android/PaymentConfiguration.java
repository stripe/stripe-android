package com.stripe.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.stripe.android.model.Address;
import com.stripe.android.model.ShippingMethod;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

public class PaymentConfiguration {

    private static PaymentConfiguration mInstance;

    private @Nullable ClassLoader mEphemeralKeyProviderClassLoader;
    private @NonNull String mPublishableKey;
    private @Address.RequiredBillingAddressFields
    int mRequiredBillingAddressFields;
    private boolean mShouldUseSourcesForCards;
    private List<ShippingMethod> mShippingMethods = new ArrayList<>();
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

    public boolean getShouldUseSourcesForCards() {
        return mShouldUseSourcesForCards;
    }

    @NonNull
    public PaymentConfiguration setShouldUseSourcesForCards(boolean shouldUseSourcesForCards) {
        mShouldUseSourcesForCards = shouldUseSourcesForCards;
        return this;
    }

    /**
     * Set the shipping methods to allow for payments made with this configuration.
     */
    public PaymentConfiguration setShippingMethods(List<ShippingMethod> shippingMethods) {
        mShippingMethods = shippingMethods;
        return this;
    }

    /**
     * @return The shipping methods to allow for payments made with this configuration.
     */
    public List<ShippingMethod> getShippingMethods() {
        return mShippingMethods;
    }

    /**
     * @return the currency that prices will be rendered in.
     */
    public Currency getCurrency() {
        return mCurrency;
    }

    /**
     * Sets the currency that prices will be rendered in.
     */
    public void setCurrency(Currency currency) {
        mCurrency = currency;
    }

    @VisibleForTesting
    static void setInstance(@Nullable PaymentConfiguration paymentConfiguration) {
        mInstance = paymentConfiguration;
    }
}
