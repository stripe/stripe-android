package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.stripe.android.StripeNetworkUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

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

    private String mBankCode;
    private String mBranchCode;
    private String mCountry;
    private String mFingerPrint;
    private String mLast4;
    private String mMandateReference;
    private String mMandateUrl;

    private SourceSepaDebitData() {
        super();
        addStandardFields(
                FIELD_BANK_CODE,
                FIELD_BRANCH_CODE,
                FIELD_COUNTRY,
                FIELD_FINGERPRINT,
                FIELD_LAST4,
                FIELD_MANDATE_REFERENCE,
                FIELD_MANDATE_URL);
    }

    @Nullable
    public static SourceSepaDebitData fromJson(JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }

        SourceSepaDebitData sepaData = new SourceSepaDebitData();
        sepaData.setBankCode(optString(jsonObject, FIELD_BANK_CODE))
                .setBranchCode(optString(jsonObject, FIELD_BRANCH_CODE))
                .setCountry(optString(jsonObject, FIELD_COUNTRY))
                .setFingerPrint(optString(jsonObject, FIELD_FINGERPRINT))
                .setLast4(optString(jsonObject, FIELD_LAST4))
                .setMandateReference(optString(jsonObject, FIELD_MANDATE_REFERENCE))
                .setMandateUrl(optString(jsonObject, FIELD_MANDATE_URL));

        Map<String, Object> nonStandardFields =
                jsonObjectToMapWithoutKeys(jsonObject, sepaData.mStandardFields);
        if (nonStandardFields != null) {
            sepaData.setAdditionalFields(nonStandardFields);
        }
        return sepaData;
    }

    public String getBankCode() {
        return mBankCode;
    }

    public String getBranchCode() {
        return mBranchCode;
    }

    public String getCountry() {
        return mCountry;
    }

    public String getFingerPrint() {
        return mFingerPrint;
    }

    public String getLast4() {
        return mLast4;
    }

    public String getMandateReference() {
        return mMandateReference;
    }

    public String getMandateUrl() {
        return mMandateUrl;
    }

    @NonNull
    @Override
    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
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
        Map<String, Object> objectMap = new HashMap<>();
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

    private SourceSepaDebitData setBankCode(String bankCode) {
        mBankCode = bankCode;
        return this;
    }

    private SourceSepaDebitData setBranchCode(String branchCode) {
        mBranchCode = branchCode;
        return this;
    }

    private SourceSepaDebitData setCountry(String country) {
        mCountry = country;
        return this;
    }

    private SourceSepaDebitData setFingerPrint(String fingerPrint) {
        mFingerPrint = fingerPrint;
        return this;
    }

    private SourceSepaDebitData setLast4(String last4) {
        mLast4 = last4;
        return this;
    }

    private SourceSepaDebitData setMandateReference(String mandateReference) {
        mMandateReference = mandateReference;
        return this;
    }

    private SourceSepaDebitData setMandateUrl(String mandateUrl) {
        mMandateUrl = mandateUrl;
        return this;
    }

}
