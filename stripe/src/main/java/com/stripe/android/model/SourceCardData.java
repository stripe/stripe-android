package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.support.annotation.VisibleForTesting;

import com.stripe.android.StripeNetworkUtils;
import com.stripe.android.utils.ObjectUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
            RECOMMENDED,
            UNKNOWN
    })
    public @interface ThreeDSecureStatus { }
    public static final String REQUIRED = "required";
    public static final String OPTIONAL = "optional";
    public static final String NOT_SUPPORTED = "not_supported";
    public static final String RECOMMENDED = "recommended";
    public static final String UNKNOWN = "unknown";

    private static final String FIELD_ADDRESS_LINE1_CHECK = "address_line1_check";
    private static final String FIELD_ADDRESS_ZIP_CHECK = "address_zip_check";
    private static final String FIELD_BRAND = "brand";
    private static final String FIELD_COUNTRY = "country";
    private static final String FIELD_CVC_CHECK = "cvc_check";
    private static final String FIELD_DYNAMIC_LAST4 = "dynamic_last4";
    private static final String FIELD_EXP_MONTH = "exp_month";
    private static final String FIELD_EXP_YEAR = "exp_year";
    private static final String FIELD_FUNDING = "funding";
    private static final String FIELD_LAST4 = "last4";
    private static final String FIELD_THREE_D_SECURE = "three_d_secure";
    private static final String FIELD_TOKENIZATION_METHOD = "tokenization_method";

    private static final Set<String> STANDARD_FIELDS = new HashSet<>(Arrays.asList(
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
            FIELD_TOKENIZATION_METHOD));

    @Nullable private final String mAddressLine1Check;
    @Nullable private final String mAddressZipCheck;
    @Nullable @Card.CardBrand private final String mBrand;
    @Nullable private final String mCountry;
    @Nullable private final String mCvcCheck;
    @Nullable private final String mDynamicLast4;
    @Nullable private final Integer mExpiryMonth;
    @Nullable private final Integer mExpiryYear;
    @Nullable @Card.FundingType private final String mFunding;
    @Nullable private final String mLast4;
    @Nullable @ThreeDSecureStatus private final String mThreeDSecureStatus;
    @Nullable private final String mTokenizationMethod;

    private SourceCardData(@NonNull Builder builder) {
        super(builder);
        
        mAddressLine1Check = builder.mAddressLine1Check;
        mAddressZipCheck = builder.mAddressZipCheck;
        mBrand = builder.mBrand;
        mCountry = builder.mCountry;
        mCvcCheck = builder.mCvcCheck;
        mDynamicLast4 = builder.mDynamicLast4;
        mExpiryMonth = builder.mExpiryMonth;
        mExpiryYear = builder.mExpiryYear;
        mFunding = builder.mFunding;
        mLast4 = builder.mLast4;
        mThreeDSecureStatus = builder.mThreeDSecureStatus;
        mTokenizationMethod = builder.mTokenizationMethod;
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
        final JSONObject jsonObject = super.toJson();
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
        return jsonObject;
    }

    @NonNull
    @Override
    public Map<String, Object> toMap() {
        final AbstractMap<String, Object> map = new HashMap<>(super.toMap());
        map.put(FIELD_ADDRESS_LINE1_CHECK, mAddressLine1Check);
        map.put(FIELD_ADDRESS_ZIP_CHECK, mAddressZipCheck);
        map.put(FIELD_BRAND, mBrand);
        map.put(FIELD_COUNTRY, mCountry);
        map.put(FIELD_DYNAMIC_LAST4, mDynamicLast4);
        map.put(FIELD_EXP_MONTH, mExpiryMonth);
        map.put(FIELD_EXP_YEAR, mExpiryYear);
        map.put(FIELD_FUNDING, mFunding);
        map.put(FIELD_LAST4, mLast4);
        map.put(FIELD_THREE_D_SECURE, mThreeDSecureStatus);
        map.put(FIELD_TOKENIZATION_METHOD, mTokenizationMethod);
        StripeNetworkUtils.removeNullAndEmptyParams(map);
        return map;
    }

    @Nullable
    static SourceCardData fromJson(@Nullable JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }

        final Builder cardData = new Builder()
                .setAddressLine1Check(optString(jsonObject, FIELD_ADDRESS_LINE1_CHECK))
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

        final Map<String, Object> nonStandardFields =
                jsonObjectToMapWithoutKeys(jsonObject, STANDARD_FIELDS);
        if (nonStandardFields != null) {
            cardData.setAdditionalFields(nonStandardFields);
        }

        return cardData.build();
    }

    @VisibleForTesting
    static SourceCardData fromString(String jsonString) {
        try {
            return fromJson(new JSONObject(jsonString));
        } catch (JSONException badJson) {
            return null;
        }
    }

    @VisibleForTesting
    @Nullable
    @ThreeDSecureStatus
    static String asThreeDSecureStatus(@Nullable String threeDSecureStatus) {
        if (StripeJsonUtils.nullIfNullOrEmpty(threeDSecureStatus) == null) {
            return null;
        }

        if (REQUIRED.equalsIgnoreCase(threeDSecureStatus)) {
            return REQUIRED;
        } else if (OPTIONAL.equalsIgnoreCase(threeDSecureStatus)) {
            return OPTIONAL;
        } else if (NOT_SUPPORTED.equalsIgnoreCase(threeDSecureStatus)) {
            return NOT_SUPPORTED;
        } else if (RECOMMENDED.equalsIgnoreCase(threeDSecureStatus)) {
            return RECOMMENDED;
        } else {
            return UNKNOWN;
        }
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return this == obj || (obj instanceof SourceCardData && typedEquals((SourceCardData) obj));
    }

    boolean typedEquals(@NonNull SourceCardData sourceCardData) {
        return super.typedEquals(sourceCardData)
                && ObjectUtils.equals(mAddressLine1Check, sourceCardData.mAddressLine1Check)
                && ObjectUtils.equals(mAddressZipCheck, sourceCardData.mAddressZipCheck)
                && ObjectUtils.equals(mBrand, sourceCardData.mBrand)
                && ObjectUtils.equals(mCountry, sourceCardData.mCountry)
                && ObjectUtils.equals(mCvcCheck, sourceCardData.mCvcCheck)
                && ObjectUtils.equals(mDynamicLast4, sourceCardData.mDynamicLast4)
                && ObjectUtils.equals(mExpiryMonth, sourceCardData.mExpiryMonth)
                && ObjectUtils.equals(mExpiryYear, sourceCardData.mExpiryYear)
                && ObjectUtils.equals(mFunding, sourceCardData.mFunding)
                && ObjectUtils.equals(mLast4, sourceCardData.mLast4)
                && ObjectUtils.equals(mThreeDSecureStatus, sourceCardData.mThreeDSecureStatus)
                && ObjectUtils.equals(mTokenizationMethod, sourceCardData.mTokenizationMethod);
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hash(mAddressLine1Check, mAddressZipCheck, mBrand, mCountry, mCvcCheck,
                mDynamicLast4, mExpiryMonth, mExpiryYear, mFunding, mLast4, mThreeDSecureStatus,
                mTokenizationMethod);
    }

    private static final class Builder extends BaseBuilder {
        private String mAddressLine1Check;
        private String mAddressZipCheck;
        @Card.CardBrand private String mBrand;
        private String mCountry;
        private String mCvcCheck;
        private String mDynamicLast4;
        private Integer mExpiryMonth;
        private Integer mExpiryYear;
        @Card.FundingType private String mFunding;
        private String mLast4;
        @ThreeDSecureStatus private String mThreeDSecureStatus;
        private String mTokenizationMethod;

        @NonNull
        private Builder setAddressLine1Check(String addressLine1Check) {
            mAddressLine1Check = addressLine1Check;
            return this;
        }

        @NonNull
        private Builder setAddressZipCheck(String addressZipCheck) {
            mAddressZipCheck = addressZipCheck;
            return this;
        }

        @NonNull
        private Builder setBrand(String brand) {
            mBrand = brand;
            return this;
        }

        @NonNull
        private Builder setCountry(String country) {
            mCountry = country;
            return this;
        }

        @NonNull
        private Builder setCvcCheck(String cvcCheck) {
            mCvcCheck = cvcCheck;
            return this;
        }

        @NonNull
        private Builder setDynamicLast4(String dynamicLast4) {
            mDynamicLast4 = dynamicLast4;
            return this;
        }

        @NonNull
        private Builder setExpiryMonth(Integer expiryMonth) {
            mExpiryMonth = expiryMonth;
            return this;
        }

        @NonNull
        private Builder setExpiryYear(Integer expiryYear) {
            mExpiryYear = expiryYear;
            return this;
        }

        @NonNull
        private Builder setFunding(String funding) {
            mFunding = funding;
            return this;
        }

        @NonNull
        private Builder setLast4(String last4) {
            mLast4 = last4;
            return this;
        }

        @NonNull
        private Builder setThreeDSecureStatus(String threeDSecureStatus) {
            mThreeDSecureStatus = threeDSecureStatus;
            return this;
        }

        @NonNull
        private Builder setTokenizationMethod(String tokenizationMethod) {
            mTokenizationMethod = tokenizationMethod;
            return this;
        }
        
        @NonNull
        public SourceCardData build() {
            return new SourceCardData(this);
        }
    }
}
