package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;
import android.support.annotation.StringDef;

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

    @Nullable private String mAccountHolderName;
    @Nullable @BankAccountType private String mAccountHolderType;
    @Nullable private String mAccountNumber;
    @Nullable private String mBankName;
    @Nullable @Size(2) private String mCountryCode;
    @Nullable @Size(3) private String mCurrency;
    @Nullable private String mFingerprint;
    @Nullable private String mLast4;
    @Nullable private String mRoutingNumber;

    /**
     * Constructor used to create a BankAccount object with the required parameters
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
            @NonNull String routingNumber) {
        mAccountNumber = accountNumber;
        mCountryCode = countryCode;
        mCurrency = currency;
        mRoutingNumber = routingNumber;
    }

    /**
     * Constructor with no account number used internally to initialize an object
     * from JSON returned from the server.
     *
     * @param accountHolderName the account holder's name
     * @param accountHolderType the {@link BankAccountType}
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
            @Nullable String bankName,
            @Nullable @Size(2) String countryCode,
            @Nullable @Size(3) String currency,
            @Nullable String fingerprint,
            @Nullable String last4,
            @Nullable String routingNumber) {
        mAccountHolderName = accountHolderName;
        mAccountHolderType = accountHolderType;
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
}
