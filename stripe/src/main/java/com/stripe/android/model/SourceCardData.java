package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.support.annotation.VisibleForTesting;

import com.stripe.android.StripeNetworkUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.Map;

import static com.stripe.android.model.StripeJsonUtils.optInteger;
import static com.stripe.android.model.StripeJsonUtils.optString;
import static com.stripe.android.model.StripeJsonUtils.putIntegerIfNotNull;
import static com.stripe.android.model.StripeJsonUtils.putStringIfNotNull;

/**
 * Model for data contained in the SourceTypeData of a Card Source.
 */
public class SourceCardData extends StripeSourceTypeModel {

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            REQUIRED,
            OPTIONAL,
            NOT_SUPPORTED,
            UNKNOWN
    })
    public @interface ThreeDSecureStatus { }
    public static final String REQUIRED = "required";
    public static final String OPTIONAL = "optional";
    public static final String NOT_SUPPORTED = "not_supported";
    public static final String UNKNOWN = "unknown";

    public static final String FIELD_ADDRESS_LINE1_CHECK = "address_line1_check";
    public static final String FIELD_ADDRESS_ZIP_CHECK = "address_zip_check";
    public static final String FIELD_BRAND = "brand";
    public static final String FIELD_COUNTRY = "country";
    public static final String FIELD_CVC_CHECK = "cvc_check";
    public static final String FIELD_DYNAMIC_LAST4 = "dynamic_last4";
    public static final String FIELD_EXP_MONTH = "exp_month";
    public static final String FIELD_EXP_YEAR = "exp_year";
    public static final String FIELD_FUNDING = "funding";
    public static final String FIELD_LAST4 = "last4";
    public static final String FIELD_THREE_D_SECURE = "three_d_secure";
    public static final String FIELD_TOKENIZATION_METHOD = "tokenization_method";

    private String mAddressLine1Check;
    private String mAddressZipCheck;
    private @Card.CardBrand String mBrand;
    private String mCountry;
    private String mCvcCheck;
    private String mDynamicLast4;
    private Integer mExpiryMonth;
    private Integer mExpiryYear;
    private @Card.FundingType String mFunding;
    private String mLast4;
    private @ThreeDSecureStatus String mThreeDSecureStatus;
    private String mTokenizationMethod;

    private SourceCardData() {
        super();
        addStandardFields(
                FIELD_ADDRESS_LINE1_CHECK,
                FIELD_ADDRESS_ZIP_CHECK,
                FIELD_BRAND,
                FIELD_COUNTRY,
                FIELD_CVC_CHECK,
                FIELD_DYNAMIC_LAST4,
                FIELD_EXP_MONTH,
                FIELD_EXP_YEAR,
                FIELD_FUNDING,
                FIELD_LAST4,
                FIELD_THREE_D_SECURE,
                FIELD_TOKENIZATION_METHOD);
    }

    @Nullable
    public String getAddressLine1Check() {
        return mAddressLine1Check;
    }

    @Nullable
    public String getAddressZipCheck() {
        return mAddressZipCheck;
    }

    @Card.CardBrand
    @Nullable
    public String getBrand() {
        return mBrand;
    }

    @Nullable
    public String getCountry() {
        return mCountry;
    }

    @Nullable
    public String getCvcCheck() {
        return mCvcCheck;
    }

    @Nullable
    public String getDynamicLast4() {
        return mDynamicLast4;
    }

    @Nullable
    public Integer getExpiryMonth() {
        return mExpiryMonth;
    }

    @Nullable
    public Integer getExpiryYear() {
        return mExpiryYear;
    }

    @Card.FundingType
    @Nullable
    public String getFunding() {
        return mFunding;
    }

    @Nullable
    public String getLast4() {
        return mLast4;
    }

    @ThreeDSecureStatus
    @Nullable
    public String getThreeDSecureStatus() {
        return mThreeDSecureStatus;
    }

    @Nullable
    public String getTokenizationMethod() {
        return mTokenizationMethod;
    }

    @NonNull
    @Override
    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        putStringIfNotNull(jsonObject, FIELD_ADDRESS_LINE1_CHECK, mAddressLine1Check);
        putStringIfNotNull(jsonObject, FIELD_ADDRESS_ZIP_CHECK, mAddressZipCheck);
        putStringIfNotNull(jsonObject, FIELD_BRAND, mBrand);
        putStringIfNotNull(jsonObject, FIELD_COUNTRY, mCountry);
        putStringIfNotNull(jsonObject, FIELD_DYNAMIC_LAST4, mDynamicLast4);
        putIntegerIfNotNull(jsonObject, FIELD_EXP_MONTH, mExpiryMonth);
        putIntegerIfNotNull(jsonObject, FIELD_EXP_YEAR, mExpiryYear);
        putStringIfNotNull(jsonObject, FIELD_FUNDING, mFunding);
        putStringIfNotNull(jsonObject, FIELD_LAST4, mLast4);
        putStringIfNotNull(jsonObject, FIELD_THREE_D_SECURE, mThreeDSecureStatus);
        putStringIfNotNull(jsonObject, FIELD_TOKENIZATION_METHOD, mTokenizationMethod);

        putAdditionalFieldsIntoJsonObject(jsonObject, mAdditionalFields);
        return jsonObject;
    }

    @NonNull
    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> objectMap = new HashMap<>();
        objectMap.put(FIELD_ADDRESS_LINE1_CHECK, mAddressLine1Check);
        objectMap.put(FIELD_ADDRESS_ZIP_CHECK, mAddressZipCheck);
        objectMap.put(FIELD_BRAND, mBrand);
        objectMap.put(FIELD_COUNTRY, mCountry);
        objectMap.put(FIELD_DYNAMIC_LAST4, mDynamicLast4);
        objectMap.put(FIELD_EXP_MONTH, mExpiryMonth);
        objectMap.put(FIELD_EXP_YEAR, mExpiryYear);
        objectMap.put(FIELD_FUNDING, mFunding);
        objectMap.put(FIELD_LAST4, mLast4);
        objectMap.put(FIELD_THREE_D_SECURE, mThreeDSecureStatus);
        objectMap.put(FIELD_TOKENIZATION_METHOD, mTokenizationMethod);

        putAdditionalFieldsIntoMap(objectMap, mAdditionalFields);
        StripeNetworkUtils.removeNullAndEmptyParams(objectMap);
        return objectMap;
    }

    private SourceCardData setAddressLine1Check(String addressLine1Check) {
        mAddressLine1Check = addressLine1Check;
        return this;
    }

    private SourceCardData setAddressZipCheck(String addressZipCheck) {
        mAddressZipCheck = addressZipCheck;
        return this;
    }

    private SourceCardData setBrand(String brand) {
        mBrand = brand;
        return this;
    }

    private SourceCardData setCountry(String country) {
        mCountry = country;
        return this;
    }

    private SourceCardData setCvcCheck(String cvcCheck) {
        mCvcCheck = cvcCheck;
        return this;
    }

    private SourceCardData setDynamicLast4(String dynamicLast4) {
        mDynamicLast4 = dynamicLast4;
        return this;
    }

    private SourceCardData setExpiryMonth(Integer expiryMonth) {
        mExpiryMonth = expiryMonth;
        return this;
    }

    private SourceCardData setExpiryYear(Integer expiryYear) {
        mExpiryYear = expiryYear;
        return this;
    }

    private SourceCardData setFunding(String funding) {
        mFunding = funding;
        return this;
    }

    private SourceCardData setLast4(String last4) {
        mLast4 = last4;
        return this;
    }

    private SourceCardData setThreeDSecureStatus(String threeDSecureStatus) {
        mThreeDSecureStatus = threeDSecureStatus;
        return this;
    }

    private SourceCardData setTokenizationMethod(String tokenizationMethod) {
        mTokenizationMethod = tokenizationMethod;
        return this;
    }

    @Nullable
    static SourceCardData fromJson(@Nullable JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }

        SourceCardData cardData = new SourceCardData();
        cardData.setAddressLine1Check(optString(jsonObject, FIELD_ADDRESS_LINE1_CHECK))
                .setAddressZipCheck(optString(jsonObject, FIELD_ADDRESS_ZIP_CHECK))
                .setBrand(Card.asCardBrand(optString(jsonObject, FIELD_BRAND)))
                .setCountry(optString(jsonObject, FIELD_COUNTRY))
                .setCvcCheck(optString(jsonObject, FIELD_CVC_CHECK))
                .setDynamicLast4(optString(jsonObject, FIELD_DYNAMIC_LAST4))
                .setExpiryMonth(optInteger(jsonObject, FIELD_EXP_MONTH))
                .setExpiryYear(optInteger(jsonObject, FIELD_EXP_YEAR))
                .setFunding(Card.asFundingType(optString(jsonObject, FIELD_FUNDING)))
                .setLast4(optString(jsonObject, FIELD_LAST4))
                .setThreeDSecureStatus(asThreeDSecureStatus(optString(jsonObject,
                        FIELD_THREE_D_SECURE)))
                .setTokenizationMethod(optString(jsonObject, FIELD_TOKENIZATION_METHOD));

        Map<String, Object> nonStandardFields =
                jsonObjectToMapWithoutKeys(jsonObject, cardData.mStandardFields);
        if (nonStandardFields != null) {
            cardData.setAdditionalFields(nonStandardFields);
        }

        return cardData;
    }

    @VisibleForTesting
    static SourceCardData fromString(String jsonString) {
        try {
            return fromJson(new JSONObject(jsonString));
        } catch (JSONException badJson) {
            return null;
        }
    }

    @Nullable
    @ThreeDSecureStatus
    static String asThreeDSecureStatus(@Nullable String threeDSecureStatus) {
        String nullChecked = StripeJsonUtils.nullIfNullOrEmpty(threeDSecureStatus);
        if (nullChecked == null) {
            return null;
        }

        if (REQUIRED.equalsIgnoreCase(threeDSecureStatus)) {
            return REQUIRED;
        } else if (OPTIONAL.equalsIgnoreCase(threeDSecureStatus)) {
            return OPTIONAL;
        } else if (NOT_SUPPORTED.equalsIgnoreCase(threeDSecureStatus)) {
            return NOT_SUPPORTED;
        } else {
            return UNKNOWN;
        }
    }
}
