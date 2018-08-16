package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;
import android.support.annotation.StringDef;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Model class representing a bank account that can be used to create a token
 * via the protocol outlined in
 * <a href="https://stripe.com/docs/api/java#create_bank_account_token">the Stripe
 * documentation.</a>
 */
public class BankAccount {

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({TYPE_COMPANY, TYPE_INDIVIDUAL})
    public @interface BankAccountType {}
    public static final String TYPE_COMPANY = "company";
    public static final String TYPE_INDIVIDUAL = "individual";

    private static final String FIELD_ACCOUNT_HOLDER_NAME = "account_holder_name";
    private static final String FIELD_ACCOUNT_HOLDER_TYPE = "account_holder_type";
    private static final String FIELD_BANK_NAME = "bank_name";
    private static final String FIELD_COUNTRY = "country";
    private static final String FIELD_CURRENCY = "currency";
    private static final String FIELD_FINGERPRINT = "fingerprint";
    private static final String FIELD_LAST4 = "last4";
    private static final String FIELD_ROUTING_NUMBER = "routing_number";

    @Nullable private String mAccountHolderName;
    @Nullable @BankAccountType private String mAccountHolderType;
    @Nullable private String mAccountNumber;
    @Nullable private String mBankName;
    @Nullable @Size(2) private String mCountryCode;
    @Nullable @Size(3) private String mCurrency;
    @Nullable private String mFingerprint;
    @Nullable private String mLast4;
    @Nullable private String mRoutingNumber;

    public static Builder newBuilder(
            @NonNull String accountNumber,
            @NonNull @Size(2) String countryCode,
            @NonNull @Size(3) String currency,
            @Nullable String routingNumber) {
        return new Builder(accountNumber, routingNumber, countryCode, currency);
    }

    public static final class Builder {
        private String accountHolderName;
        private String accountHolderType;
        private String accountNumber;
        private String bankName;
        private String countryCode;
        private String currency;
        private String fingerprint;
        private String last4;
        private String routingNumber;

        private Builder(
                @NonNull String accountNumber,
                @Nullable String routingNumber,
                @NonNull @Size(2) String countryCode,
                @NonNull @Size(3) String currency
        ) {
            this.accountNumber = accountNumber;
            this.routingNumber = routingNumber;
            this.countryCode = countryCode;
            this.currency = currency;
        }

        public Builder setAccountHolderName(String mAccountHolderName) {
            this.accountHolderName = mAccountHolderName;
            return this;
        }

        public Builder setAccountHolderType(String mAccountHolderType) {
            this.accountHolderType = mAccountHolderType;
            return this;
        }

        public Builder setAccountNumber(String mAccountNumber) {
            this.accountNumber = mAccountNumber;
            return this;
        }

        public Builder setBankName(String mBankName) {
            this.bankName = mBankName;
            return this;
        }

        public Builder setCountryCode(String mCountryCode) {
            this.countryCode = mCountryCode;
            return this;
        }

        public Builder setCurrency(String mCurrency) {
            this.currency = mCurrency;
            return this;
        }

        public Builder setFingerprint(String mFingerprint) {
            this.fingerprint = mFingerprint;
            return this;
        }

        public Builder setLast4(String mLast4) {
            this.last4 = mLast4;
            return this;
        }

        public Builder setRoutingNumber(String mRoutingNumber) {
            this.routingNumber = mRoutingNumber;
            return this;
        }

        public BankAccount build() {
            return new BankAccount(this);
        }
    }

    /**
     * Convenience constructor used to create a BankAccount object with the required parameters
     * to send to Stripe's server.
     *
     * @param accountNumber the account number for this BankAccount
     * @param countryCode the two-letter country code that this account was created in
     * @param currency the currency of this account
     * @param routingNumber the routing number of this account
     */
    public BankAccount(
            @NonNull String accountNumber,
            @NonNull @Size(2) String countryCode,
            @NonNull @Size(3) String currency,
            @Nullable String routingNumber) {
        this(
                null,
                null,
                accountNumber,
                null,
                countryCode,
                currency,
                null,
                null,
                routingNumber
        );
    }

    /**
     * Convenience constructor used to create a BankAccount object with the required and
     * optional parameters to send to Stripe's server.
     *
     * @param accountNumber the account number for this BankAccount
     * @param countryCode the two-letter country code that this account was created in
     * @param currency the currency of this account
     * @param routingNumber the routing number of this account
     */
    public BankAccount(
            @NonNull String accountNumber,
            @Nullable String routingNumber,
            @NonNull @Size(2) String countryCode,
            @NonNull @Size(3) String currency,
            @Nullable String accountHolderName,
            @Nullable @BankAccountType String accountHolderType) {
        this(
                accountHolderName,
                accountHolderType,
                accountNumber,
                null,
                countryCode,
                currency,
                null,
                null,
                routingNumber
        );
    }

    /**
     * Convenience constructor with all available parameters.
     *
     * @param accountHolderName the account holder's name
     * @param accountHolderType the {@link BankAccountType}
     * @param accountNumber the account number for this BankAccount
     * @param bankName the name of the bank
     * @param countryCode the two-letter country code of the country in which the account was opened
     * @param currency the three-letter currency code
     * @param fingerprint the account fingerprint
     * @param last4 the last four digits of the account number
     * @param routingNumber the routing number of the bank
     */
    public BankAccount(
            @Nullable String accountHolderName,
            @Nullable @BankAccountType String accountHolderType,
            @Nullable String accountNumber,
            @Nullable String bankName,
            @Nullable @Size(2) String countryCode,
            @Nullable @Size(3) String currency,
            @Nullable String fingerprint,
            @Nullable String last4,
            @Nullable String routingNumber) {
        mAccountHolderName = accountHolderName;
        mAccountHolderType = accountHolderType;
        mAccountNumber = accountNumber;
        mBankName = bankName;
        mCountryCode = countryCode;
        mCurrency = currency;
        mFingerprint = fingerprint;
        mLast4 = last4;
        mRoutingNumber = routingNumber;
    }

    @Nullable
    public String getAccountNumber() {
        return mAccountNumber;
    }

    @Nullable
    public String getAccountHolderName() {
        return mAccountHolderName;
    }

    @NonNull
    public BankAccount setAccountHolderName(String accountHolderName) {
        mAccountHolderName = accountHolderName;
        return this;
    }

    @Nullable
    @BankAccountType
    public String getAccountHolderType() {
        return mAccountHolderType;
    }

    @NonNull
    public BankAccount setAccountHolderType(@BankAccountType String accountHolderType) {
        mAccountHolderType = accountHolderType;
        return this;
    }

    @Nullable
    public String getBankName() {
        return mBankName;
    }

    @Nullable
    @Size(2)
    public String getCountryCode() {
        return mCountryCode;
    }

    @Nullable
    @Size(3)
    public String getCurrency() {
        return mCurrency;
    }

    @Nullable
    public String getFingerprint() {
        return mFingerprint;
    }

    @Nullable
    public String getLast4() {
        return mLast4;
    }

    @Nullable
    public String getRoutingNumber() {
        return mRoutingNumber;
    }

    /**
     * Converts a String value into the appropriate {@link BankAccountType}.
     *
     * @param possibleAccountType a String that might match a {@link BankAccountType} or be empty.
     * @return {@code null} if the input is blank or of unknown type, else the appropriate
     *         {@link BankAccountType}.
     */
    @Nullable
    @BankAccountType
    public static String asBankAccountType(@Nullable String possibleAccountType) {
        if (BankAccount.TYPE_COMPANY.equals(possibleAccountType)) {
            return BankAccount.TYPE_COMPANY;
        } else if (BankAccount.TYPE_INDIVIDUAL.equals(possibleAccountType)) {
            return BankAccount.TYPE_INDIVIDUAL;
        }

        return null;
    }

    @Nullable
    public static BankAccount fromString(@Nullable String jsonString) {
        try {
            JSONObject accountObject = new JSONObject(jsonString);
            return fromJson(accountObject);
        } catch (JSONException jsonException) {
            return null;
        }
    }

    @Nullable
    public static BankAccount fromJson(@Nullable JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }

        return new BankAccount(
                StripeJsonUtils.optString(jsonObject, FIELD_ACCOUNT_HOLDER_NAME),
                asBankAccountType(
                        StripeJsonUtils.optString(jsonObject, FIELD_ACCOUNT_HOLDER_TYPE)),
                null,
                StripeJsonUtils.optString(jsonObject, FIELD_BANK_NAME),
                StripeJsonUtils.optCountryCode(jsonObject, FIELD_COUNTRY),
                StripeJsonUtils.optCurrency(jsonObject, FIELD_CURRENCY),
                StripeJsonUtils.optString(jsonObject, FIELD_FINGERPRINT),
                StripeJsonUtils.optString(jsonObject, FIELD_LAST4),
                StripeJsonUtils.optString(jsonObject, FIELD_ROUTING_NUMBER));
    }

    private BankAccount(Builder builder) {
        mAccountHolderName = builder.accountHolderName;
        mAccountHolderType = builder.accountHolderType;
        mAccountNumber = builder.accountNumber;
        mBankName = builder.bankName;
        mCountryCode = builder.countryCode;
        mCurrency = builder.currency;
        mFingerprint = builder.fingerprint;
        mLast4 = builder.last4;
        mRoutingNumber = builder.routingNumber;
    }

}
