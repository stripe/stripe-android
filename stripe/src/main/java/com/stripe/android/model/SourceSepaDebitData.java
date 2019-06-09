package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.stripe.android.StripeNetworkUtils;
import com.stripe.android.utils.ObjectUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.stripe.android.model.StripeJsonUtils.optString;

/**
 * Model for the SourceTypeData contained in a SEPA Debit Source object.
 */
public class SourceSepaDebitData extends StripeSourceTypeModel {

    private static final String FIELD_BANK_CODE = "bank_code";
    private static final String FIELD_BRANCH_CODE = "branch_code";
    private static final String FIELD_COUNTRY = "country";
    private static final String FIELD_FINGERPRINT = "fingerprint";
    private static final String FIELD_LAST4 = "last4";
    private static final String FIELD_MANDATE_REFERENCE = "mandate_reference";
    private static final String FIELD_MANDATE_URL = "mandate_url";

    private static final Set<String> STANDARD_FIELDS = new HashSet<>(Arrays.asList(
            FIELD_BANK_CODE,
            FIELD_BRANCH_CODE,
            FIELD_COUNTRY,
            FIELD_FINGERPRINT,
            FIELD_LAST4,
            FIELD_MANDATE_REFERENCE,
            FIELD_MANDATE_URL));

    @Nullable private final String mBankCode;
    @Nullable private final String mBranchCode;
    @Nullable private final String mCountry;
    @Nullable private final String mFingerPrint;
    @Nullable private final String mLast4;
    @Nullable private final String mMandateReference;
    @Nullable private final String mMandateUrl;

    private SourceSepaDebitData(@NonNull Builder builder) {
        super(builder);
        mBankCode = builder.mBankCode;
        mBranchCode = builder.mBranchCode;
        mCountry = builder.mCountry;
        mFingerPrint = builder.mFingerPrint;
        mLast4 = builder.mLast4;
        mMandateReference = builder.mMandateReference;
        mMandateUrl = builder.mMandateUrl;
    }

    @Nullable
    public static SourceSepaDebitData fromJson(JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }

        final Builder sepaData = new Builder()
                .setBankCode(optString(jsonObject, FIELD_BANK_CODE))
                .setBranchCode(optString(jsonObject, FIELD_BRANCH_CODE))
                .setCountry(optString(jsonObject, FIELD_COUNTRY))
                .setFingerPrint(optString(jsonObject, FIELD_FINGERPRINT))
                .setLast4(optString(jsonObject, FIELD_LAST4))
                .setMandateReference(optString(jsonObject, FIELD_MANDATE_REFERENCE))
                .setMandateUrl(optString(jsonObject, FIELD_MANDATE_URL));

        final Map<String, Object> nonStandardFields =
                jsonObjectToMapWithoutKeys(jsonObject, STANDARD_FIELDS);
        if (nonStandardFields != null) {
            sepaData.setAdditionalFields(nonStandardFields);
        }
        return sepaData.build();
    }

    @Nullable
    public String getBankCode() {
        return mBankCode;
    }

    @Nullable
    public String getBranchCode() {
        return mBranchCode;
    }

    @Nullable
    public String getCountry() {
        return mCountry;
    }

    @Nullable
    public String getFingerPrint() {
        return mFingerPrint;
    }

    @Nullable
    public String getLast4() {
        return mLast4;
    }

    @Nullable
    public String getMandateReference() {
        return mMandateReference;
    }

    @Nullable
    public String getMandateUrl() {
        return mMandateUrl;
    }

    @NonNull
    @Override
    public Map<String, Object> toMap() {
        final AbstractMap<String, Object> map = new HashMap<>(super.toMap());
        map.put(FIELD_BANK_CODE, mBankCode);
        map.put(FIELD_BRANCH_CODE, mBranchCode);
        map.put(FIELD_COUNTRY, mCountry);
        map.put(FIELD_FINGERPRINT, mFingerPrint);
        map.put(FIELD_LAST4, mLast4);
        map.put(FIELD_MANDATE_REFERENCE, mMandateReference);
        map.put(FIELD_MANDATE_URL, mMandateUrl);
        StripeNetworkUtils.removeNullAndEmptyParams(map);
        return map;
    }

    @Nullable
    @VisibleForTesting
    static SourceSepaDebitData fromString(String jsonString) {
        try {
            return fromJson(new JSONObject(jsonString));
        } catch (JSONException badJson) {
            return null;
        }
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj
                || (obj instanceof SourceSepaDebitData && typedEquals((SourceSepaDebitData) obj));
    }

    private boolean typedEquals(@NonNull SourceSepaDebitData obj) {
        return super.typedEquals(obj)
                && ObjectUtils.equals(mBankCode, obj.mBankCode)
                && ObjectUtils.equals(mBranchCode, obj.mBranchCode)
                && ObjectUtils.equals(mCountry, obj.mCountry)
                && ObjectUtils.equals(mFingerPrint, obj.mFingerPrint)
                && ObjectUtils.equals(mLast4, obj.mLast4)
                && ObjectUtils.equals(mMandateReference, obj.mMandateReference)
                && ObjectUtils.equals(mMandateUrl, obj.mMandateUrl);
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hash(super.hashCode(), mBankCode, mBranchCode, mCountry, mFingerPrint,
                mLast4, mMandateReference, mMandateUrl);
    }

    public static final class Builder extends BaseBuilder {
        private String mBankCode;
        private String mBranchCode;
        private String mCountry;
        private String mFingerPrint;
        private String mLast4;
        private String mMandateReference;
        private String mMandateUrl;

        @NonNull
        Builder setBankCode(String bankCode) {
            mBankCode = bankCode;
            return this;
        }

        @NonNull
        Builder setBranchCode(String branchCode) {
            mBranchCode = branchCode;
            return this;
        }

        @NonNull
        Builder setCountry(String country) {
            mCountry = country;
            return this;
        }

        @NonNull
        Builder setFingerPrint(String fingerPrint) {
            mFingerPrint = fingerPrint;
            return this;
        }

        @NonNull
        Builder setLast4(String last4) {
            mLast4 = last4;
            return this;
        }

        @NonNull
        Builder setMandateReference(String mandateReference) {
            mMandateReference = mandateReference;
            return this;
        }

        @NonNull
        Builder setMandateUrl(String mandateUrl) {
            mMandateUrl = mandateUrl;
            return this;
        }
        
        @NonNull
        public SourceSepaDebitData build() {
            return new SourceSepaDebitData(this);
        }
    }
}
