package com.stripe.wrap.pay;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import com.google.android.gms.identity.intents.model.CountrySpecification;
import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.FullWalletRequest;
import com.google.android.gms.wallet.MaskedWallet;
import com.google.android.gms.wallet.MaskedWalletRequest;
import com.google.android.gms.wallet.PaymentMethodTokenizationParameters;

import com.google.android.gms.wallet.WalletConstants;
import com.stripe.android.model.Source;
import com.stripe.android.model.Token;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

import static com.google.android.gms.wallet.PaymentMethodTokenizationType.PAYMENT_GATEWAY;

/**
 * Singleton class that represents one-time configuration data for Android Pay integration.
 */
public class AndroidPayConfiguration {

    private static AndroidPayConfiguration mInstance;

    @NonNull private List<Integer> mCardNetworks = new ArrayList<>();
    @NonNull private List<CountrySpecification> mCountrySpecifications = new ArrayList<>();
    @Nullable private String mCountryCode;
    @NonNull private Currency mCurrency;
    private boolean mIsPhoneNumberRequired;
    private boolean mIsShippingAddressRequired;
    private boolean mUsesSources;
    private String mMerchantName;
    private String mPublicApiKey;

    @NonNull
    public static AndroidPayConfiguration getInstance() {
        if (mInstance == null) {
            throw new RuntimeException(
                    "Attempted to get instance of AndroidPayConfiguration without initialization.");
        }
        return mInstance;
    }

    /**
     * Initialize the AndroidPayConfiguration instance with a currency code. If the currency
     * code is invalid, this will throw an {@link IllegalArgumentException}
     *
     * @param publicApiKey the Stripe api public key
     * @param currencyCode the code for the starting {@link Currency} for android pay configuration
     * @param shouldUseSources {@code true} if you prefer to use {@link Source} objects
     * for payment processing. Defaults to {@code false}, which uses {@link Token} objects.
     * @return the instance of the AndroidPayConfiguration singleton
     */
    public static AndroidPayConfiguration init(
            @NonNull @Size(min = 5) String publicApiKey,
            @NonNull String currencyCode,
            boolean shouldUseSources) {
        mInstance = new AndroidPayConfiguration(
                publicApiKey,
                Currency.getInstance(currencyCode.toUpperCase()),
                shouldUseSources);
        return mInstance;
    }

    /**
     * Initialize the AndroidPayConfiguration instance with a currency code. If the currency
     * code is invalid, this will throw an {@link IllegalArgumentException}
     *
     * @param publicApiKey the Stripe api public key
     * @param currencyCode the code for the starting {@link Currency} for android pay configuration
     * @return the instance of the AndroidPayConfiguration singleton
     */
    public static AndroidPayConfiguration init(
            @NonNull @Size(min = 5) String publicApiKey,
            @NonNull String currencyCode) {
        return init(publicApiKey, Currency.getInstance(currencyCode.toUpperCase()));
    }

    /**
     * Initialize the AndroidPayConfiguration instance with a {@link Currency}.
     *
     * @param publicApiKey the Stripe public api key
     * @param currency the starting {@link Currency} for android pay configuration
     * @return the instance of the AndroidPayConfiguration singleton
     */
    public static AndroidPayConfiguration init(
            @NonNull @Size(min = 5) String publicApiKey,
            @NonNull Currency currency) {
        mInstance = new AndroidPayConfiguration(publicApiKey, currency, false);
        return mInstance;
    }

    @VisibleForTesting
    AndroidPayConfiguration(
            @NonNull String publicApiKey,
            @NonNull Currency currency,
            boolean shouldUseSources) {
        mCurrency = currency;
        mPublicApiKey = publicApiKey;
        mUsesSources = shouldUseSources;
    }

    /**
     * Specifies a set of card networks to allow. If not explicitly set,
     * the default supported networks will be AMEX, DISCOVER, MASTERCARD, and VISA, as per
     * <a href="https://developers.google.com/android/reference/com/google/android/gms/wallet/MaskedWalletRequest.Builder.html#addAllowedCardNetwork(int)">
     * Google's specifications</a>.
     *
     * @param networks one or more {@link WalletConstants.CardNetwork} values
     */
    public void addAllowedCardNetworks(@WalletConstants.CardNetwork Integer... networks) {
        mCardNetworks.addAll(Arrays.asList(networks));
    }

    /**
     * Add allowed shipping countries to your Masked Wallet Requests.
     *
     * @param countrySpecifications one or more ISO 3166-1 alpha-2 country codes
     */
    public void addAllowedShippingCountries(@NonNull String... countrySpecifications) {
        for (String countrySpecification : countrySpecifications) {
            mCountrySpecifications.add(new CountrySpecification(countrySpecification));
        }
    }

    /**
     * Add allowed shipping countries to your Masked Wallet Requests.
     *
     * @param countrySpecifications one or more {@link CountrySpecification} objects
     */
    public void addAllowedShippingCountries(
            @NonNull CountrySpecification... countrySpecifications) {
        mCountrySpecifications.addAll(Arrays.asList(countrySpecifications));
    }

    @Nullable
    public PaymentMethodTokenizationParameters getPaymentMethodTokenizationParameters() {
        if (TextUtils.isEmpty(mPublicApiKey)) {
            return null;
        }
        return PaymentMethodTokenizationParameters.newBuilder()
                    .setPaymentMethodTokenizationType(PAYMENT_GATEWAY)
                    .addParameter("gateway", "stripe")
                    .addParameter("stripe:publishableKey", mPublicApiKey)
                    .addParameter("stripe:version", com.stripe.android.BuildConfig.VERSION_NAME)
                    .build();
    }

    @Nullable
    public MaskedWalletRequest generateMaskedWalletRequest(@Nullable Cart cart) {
        return generateMaskedWalletRequest(
                cart,
                mIsPhoneNumberRequired,
                mIsShippingAddressRequired,
                mCurrency.getCurrencyCode());
    }

    @NonNull
    public static FullWalletRequest generateFullWalletRequest(
            @NonNull String googleTransactionId,
            @NonNull Cart cart) {
        return FullWalletRequest.newBuilder()
                .setGoogleTransactionId(googleTransactionId)
                .setCart(cart)
                .build();
    }

    @Nullable
    public MaskedWalletRequest generateMaskedWalletRequest(
            @Nullable Cart cart,
            boolean isPhoneNumberRequired,
            boolean isShippingAddressRequired,
            @Nullable String currencyCode) {
        if (cart == null || cart.getTotalPrice() == null || currencyCode == null) {
            return null;
        }

        PaymentMethodTokenizationParameters paymentMethodTokenizationParameters
                = getPaymentMethodTokenizationParameters();
        if (paymentMethodTokenizationParameters == null) {
            return null;
        }

        MaskedWalletRequest.Builder builder = MaskedWalletRequest.newBuilder()
                .setCart(cart)
                .setPhoneNumberRequired(isPhoneNumberRequired)
                .setShippingAddressRequired(isShippingAddressRequired)
                .setMerchantName(mMerchantName)
                .setCurrencyCode(currencyCode)
                .setEstimatedTotalPrice(cart.getTotalPrice())
                .setPaymentMethodTokenizationParameters(paymentMethodTokenizationParameters);
        if (!TextUtils.isEmpty(mCountryCode)) {
            builder.setCountryCode(mCountryCode);
        }

        if (!mCountrySpecifications.isEmpty()) {
            builder.addAllowedCountrySpecificationsForShipping(mCountrySpecifications);
        }

        if (!mCardNetworks.isEmpty()) {
            builder.addAllowedCardNetworks(mCardNetworks);
        }

        return builder.build();
    }

    @NonNull
    public Currency getCurrency() {
        return mCurrency;
    }

    @Nullable
    public String getCountryCode() {
        return mCountryCode;
    }

    @NonNull
    public String getCurrencyCode() {
        return mCurrency.getCurrencyCode();
    }

    @NonNull
    public AndroidPayConfiguration setCurrency(@NonNull Currency currency) {
        mCurrency = currency;
        return this;
    }

    @NonNull
    public AndroidPayConfiguration setCurrencyCode(@NonNull String currencyCode) {
        mCurrency = Currency.getInstance(currencyCode.toUpperCase());
        return this;
    }

    /**
     * Sets the ISO 3166-1 alpha-2 country code based on where the transaction will be completed
     * or processed. Note that the input value is not validated locally.
     *
     * @param countryCode the country code to be set
     * @return {@code this, for chaining purposes}
     */
    @NonNull
    public AndroidPayConfiguration setCountryCode(@Nullable String countryCode) {
        mCountryCode = countryCode;
        return this;
    }

    public boolean isPhoneNumberRequired() {
        return mIsPhoneNumberRequired;
    }

    public AndroidPayConfiguration setPhoneNumberRequired(boolean phoneNumberRequired) {
        mIsPhoneNumberRequired = phoneNumberRequired;
        return this;
    }

    public boolean isShippingAddressRequired() {
        return mIsShippingAddressRequired;
    }

    public AndroidPayConfiguration setShippingAddressRequired(boolean shippingAddressRequired) {
        mIsShippingAddressRequired = shippingAddressRequired;
        return this;
    }

    public String getMerchantName() {
        return mMerchantName;
    }

    public AndroidPayConfiguration setMerchantName(String merchantName) {
        mMerchantName = merchantName;
        return this;
    }

    public String getPublicApiKey() {
        return mPublicApiKey;
    }

    public AndroidPayConfiguration setPublicApiKey(String publicApiKey) {
        mPublicApiKey = publicApiKey;
        return this;
    }

    public boolean getUsesSources() {
        return mUsesSources;
    }

    private void setUseSources(boolean shouldUseSources) {
        mUsesSources = shouldUseSources;
    }
}
