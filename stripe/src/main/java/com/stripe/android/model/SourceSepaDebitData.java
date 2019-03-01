package com.stripe.android.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.stripe.android.StripeNetworkUtils;
import com.stripe.android.utils.ObjectUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.stripe.android.model.StripeJsonUtils.optString;
import static com.stripe.android.model.StripeJsonUtils.putStringIfNotNull;

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
        super(STANDARD_FIELDS);
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

        final SourceSepaDebitData sepaData = new SourceSepaDebitData.Builder()
                .setBankCode(optString(jsonObject, FIELD_BANK_CODE))
                .setBranchCode(optString(jsonObject, FIELD_BRANCH_CODE))
                .setCountry(optString(jsonObject, FIELD_COUNTRY))
                .setFingerPrint(optString(jsonObject, FIELD_FINGERPRINT))
                .setLast4(optString(jsonObject, FIELD_LAST4))
                .setMandateReference(optString(jsonObject, FIELD_MANDATE_REFERENCE))
                .setMandateUrl(optString(jsonObject, FIELD_MANDATE_URL))
                .build();

        final Map<String, Object> nonStandardFields =
                jsonObjectToMapWithoutKeys(jsonObject, sepaData.mStandardFields);
        if (nonStandardFields != null) {
            sepaData.setAdditionalFields(nonStandardFields);
        }
        return sepaData;
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
    public JSONObject toJson() {
        final JSONObject jsonObject = new JSONObject();
        putStringIfNotNull(jsonObject, FIELD_BANK_CODE, mBankCode);
        putStringIfNotNull(jsonObject, FIELD_BRANCH_CODE, mBranchCode);
        putStringIfNotNull(jsonObject, FIELD_COUNTRY, mCountry);
        putStringIfNotNull(jsonObject, FIELD_FINGERPRINT, mFingerPrint);
        putStringIfNotNull(jsonObject, FIELD_LAST4, mLast4);
        putStringIfNotNull(jsonObject, FIELD_MANDATE_REFERENCE, mMandateReference);
        putStringIfNotNull(jsonObject, FIELD_MANDATE_URL, mMandateUrl);

        putAdditionalFieldsIntoJsonObject(jsonObject, mAdditionalFields);
        return jsonObject;
    }

    @NonNull
    @Override
    public Map<String, Object> toMap() {
        final Map<String, Object> objectMap = new HashMap<>();
        objectMap.put(FIELD_BANK_CODE, mBankCode);
        objectMap.put(FIELD_BRANCH_CODE, mBranchCode);
        objectMap.put(FIELD_COUNTRY, mCountry);
        objectMap.put(FIELD_FINGERPRINT, mFingerPrint);
        objectMap.put(FIELD_LAST4, mLast4);
        objectMap.put(FIELD_MANDATE_REFERENCE, mMandateReference);
        objectMap.put(FIELD_MANDATE_URL, mMandateUrl);

        putAdditionalFieldsIntoMap(objectMap, mAdditionalFields);
        StripeNetworkUtils.removeNullAndEmptyParams(objectMap);
        return objectMap;
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
        return ObjectUtils.equals(mBankCode, obj.mBankCode)
                && ObjectUtils.equals(mBranchCode, obj.mBranchCode)
                && ObjectUtils.equals(mCountry, obj.mCountry)
                && ObjectUtils.equals(mFingerPrint, obj.mFingerPrint)
                && ObjectUtils.equals(mLast4, obj.mLast4)
                && ObjectUtils.equals(mMandateReference, obj.mMandateReference)
                && ObjectUtils.equals(mMandateUrl, obj.mMandateUrl);
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hash(mBankCode, mBranchCode, mCountry, mFingerPrint, mLast4,
                mMandateReference, mMandateUrl);
    }

    public static final class Builder {
        private String mBankCode;
        private String mBranchCode;
        private String mCountry;
        private String mFingerPrint;
        private String mLast4;
        private String mMandateReference;
        private String mMandateUrl;

        @NonNull
        public Builder setBankCode(String bankCode) {
            mBankCode = bankCode;
            return this;
        }

        @NonNull
        public Builder setBranchCode(String branchCode) {
            mBranchCode = branchCode;
            return this;
        }

        @NonNull
        public Builder setCountry(String country) {
            mCountry = country;
            return this;
        }

        @NonNull
        public Builder setFingerPrint(String fingerPrint) {
            mFingerPrint = fingerPrint;
            return this;
        }

        @NonNull
        public Builder setLast4(String last4) {
            mLast4 = last4;
            return this;
        }

        @NonNull
        public Builder setMandateReference(String mandateReference) {
            mMandateReference = mandateReference;
            return this;
        }

        @NonNull
        public Builder setMandateUrl(String mandateUrl) {
            mMandateUrl = mandateUrl;
            return this;
        }
        
        @NonNull
        public SourceSepaDebitData build() {
            return new SourceSepaDebitData(this);
        }
    }

}
