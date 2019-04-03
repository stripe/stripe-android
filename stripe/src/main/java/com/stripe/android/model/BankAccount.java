package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;
import android.support.annotation.StringDef;

import com.stripe.android.utils.ObjectUtils;

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

    @Nullable private final String mAccountHolderName;
    @Nullable @BankAccountType private final String mAccountHolderType;
    @Nullable private final String mAccountNumber;
    @Nullable private final String mBankName;
    @Nullable @Size(2) private final String mCountryCode;
    @Nullable @Size(3) private final String mCurrency;
    @Nullable private final String mFingerprint;
    @Nullable private final String mLast4;
    @Nullable private final String mRoutingNumber;

    /**
     * Constructor used to create a BankAccount object with the required parameters
     * to send to Stripe's server.
     *
     * @param accountNumber the account number for this BankAccount
     * @param countryCode the two-letter country code that this account was created in
     * @param currency the currency of this account
     * @param routingNumber the routing number of this account. Can be null for non US bank
     *                      accounts.
     */
    public BankAccount(
            @NonNull String accountNumber,
            @NonNull @Size(2) String countryCode,
            @NonNull @Size(3) String currency,
            @Nullable String routingNumber) {
        this(accountNumber, null, null, null, countryCode,
                currency, null, null, routingNumber);
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
        this(null, accountHolderName, accountHolderType, bankName, countryCode,
                currency, fingerprint, last4, routingNumber);
    }

    public BankAccount(
            @Nullable String accountNumber,
            @Nullable String accountHolderName,
            @Nullable @BankAccountType String accountHolderType,
            @Nullable String bankName,
            @Nullable @Size(2) String countryCode,
            @Nullable @Size(3) String currency,
            @Nullable String fingerprint,
            @Nullable String last4,
            @Nullable String routingNumber) {
        mAccountNumber = accountNumber;
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

    @Nullable
    @BankAccountType
    public String getAccountHolderType() {
        return mAccountHolderType;
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
                StripeJsonUtils.optString(jsonObject, FIELD_BANK_NAME),
                StripeJsonUtils.optCountryCode(jsonObject, FIELD_COUNTRY),
                StripeJsonUtils.optCurrency(jsonObject, FIELD_CURRENCY),
                StripeJsonUtils.optString(jsonObject, FIELD_FINGERPRINT),
                StripeJsonUtils.optString(jsonObject, FIELD_LAST4),
                StripeJsonUtils.optString(jsonObject, FIELD_ROUTING_NUMBER));
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hash(mAccountHolderName, mAccountHolderType, mAccountNumber,
                mBankName, mCountryCode, mCurrency, mFingerprint, mLast4, mRoutingNumber);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return super.equals(obj) || (obj instanceof BankAccount && typedEquals((BankAccount) obj));
    }

    private boolean typedEquals(@NonNull BankAccount bankAccount) {
        return ObjectUtils.equals(mAccountHolderName, bankAccount.mAccountHolderName)
                && ObjectUtils.equals(mAccountHolderType, bankAccount.mAccountHolderType)
                && ObjectUtils.equals(mAccountNumber, bankAccount.mAccountNumber)
                && ObjectUtils.equals(mBankName, bankAccount.mBankName)
                && ObjectUtils.equals(mCountryCode, bankAccount.mCountryCode)
                && ObjectUtils.equals(mCurrency, bankAccount.mCurrency)
                && ObjectUtils.equals(mFingerprint, bankAccount.mFingerprint)
                && ObjectUtils.equals(mLast4, bankAccount.mLast4)
                && ObjectUtils.equals(mRoutingNumber, bankAccount.mRoutingNumber);
    }
}
