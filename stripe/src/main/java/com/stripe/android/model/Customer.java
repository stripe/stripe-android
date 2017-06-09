package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.util.StripeNetworkUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static com.stripe.android.util.StripeJsonUtils.optBoolean;
import static com.stripe.android.util.StripeJsonUtils.optCurrency;
import static com.stripe.android.util.StripeJsonUtils.optHash;
import static com.stripe.android.util.StripeJsonUtils.optInteger;
import static com.stripe.android.util.StripeJsonUtils.optLong;
import static com.stripe.android.util.StripeJsonUtils.optMap;
import static com.stripe.android.util.StripeJsonUtils.optString;
import static com.stripe.android.util.StripeJsonUtils.putBooleanIfNotNull;
import static com.stripe.android.util.StripeJsonUtils.putIntegerIfNotNull;
import static com.stripe.android.util.StripeJsonUtils.putLongIfNotNull;
import static com.stripe.android.util.StripeJsonUtils.putMapIfNotNull;
import static com.stripe.android.util.StripeJsonUtils.putStringHashIfNotNull;
import static com.stripe.android.util.StripeJsonUtils.putStringIfNotNull;

/**
 * Model for a Stripe Customer object
 */
public class Customer extends StripeJsonModel {

    private static final String FIELD_ID = "id";
    private static final String FIELD_OBJECT = "object";
    private static final String FIELD_ACCOUNT_BALANCE = "account_balance";
    private static final String FIELD_BUSINESS_VAT_ID = "business_vat_id";
    private static final String FIELD_CREATED = "created";
    private static final String FIELD_CURRENCY = "currency";
    private static final String FIELD_DEFAULT_SOURCE = "default_source";
    private static final String FIELD_DELINQUENT = "delinquent";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_DISCOUNT = "discount";
    private static final String FIELD_EMAIL = "email";
    private static final String FIELD_LIVEMODE = "livemode";
    private static final String FIELD_METADATA = "metadata";
    private static final String FIELD_SHIPPING = "shipping";
    private static final String FIELD_SOURCES = "sources";

    private static final String VALUE_OBJECT = "customer";

    private String mId;
    private Integer mAccountBalance;
    private String mBusinessVatId;

    // This is a timestamp
    private Long mCreated;

    // 3-letter ISO code in lowercase
    private String mCurrency;

    private String mDefaultSource;
    private Boolean mDelinquent;
    private String mDescription;
    private Map<String, Object> mDiscount;
    private String mEmail;
    private Boolean mLivemode;
    private Map<String, String> mMetadata;
    private ShippingInformation mShippingInformation;
    private CustomerSources mCustomerSources;

    private Customer() { }

    public String getId() {
        return mId;
    }

    public Integer getAccountBalance() {
        return mAccountBalance;
    }

    public String getBusinessVatId() {
        return mBusinessVatId;
    }

    public Long getCreated() {
        return mCreated;
    }

    public String getCurrency() {
        return mCurrency;
    }

    public String getDefaultSource() {
        return mDefaultSource;
    }

    public Boolean getDelinquent() {
        return mDelinquent;
    }

    public String getDescription() {
        return mDescription;
    }

    public Map<String, Object> getDiscount() {
        return mDiscount;
    }

    public String getEmail() {
        return mEmail;
    }

    public Boolean getLivemode() {
        return mLivemode;
    }

    public Map<String, String> getMetadata() {
        return mMetadata;
    }

    public ShippingInformation getShippingInformation() {
        return mShippingInformation;
    }

    public CustomerSources getCustomerSources() {
        return mCustomerSources;
    }

    @NonNull
    @Override
    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        putStringIfNotNull(jsonObject, FIELD_ID, mId);
        putStringIfNotNull(jsonObject, FIELD_OBJECT, VALUE_OBJECT);
        putIntegerIfNotNull(jsonObject, FIELD_ACCOUNT_BALANCE, mAccountBalance);
        putStringIfNotNull(jsonObject, FIELD_BUSINESS_VAT_ID, mBusinessVatId);
        putLongIfNotNull(jsonObject, FIELD_CREATED, mCreated);
        putStringIfNotNull(jsonObject, FIELD_DEFAULT_SOURCE, mDefaultSource);
        putBooleanIfNotNull(jsonObject, FIELD_DELINQUENT, mDelinquent);
        putStringIfNotNull(jsonObject, FIELD_DESCRIPTION, mDescription);
        putMapIfNotNull(jsonObject, FIELD_DISCOUNT, mDiscount);
        putStringIfNotNull(jsonObject, FIELD_EMAIL, mEmail);
        putBooleanIfNotNull(jsonObject, FIELD_LIVEMODE, mLivemode);
        putStringHashIfNotNull(jsonObject, FIELD_METADATA, mMetadata);
        StripeJsonModel.putStripeJsonModelIfNotNull(jsonObject,
                FIELD_SHIPPING,
                mShippingInformation);
        StripeJsonModel.putStripeJsonModelIfNotNull(
                jsonObject,
                FIELD_SOURCES,
                mCustomerSources);

        return jsonObject;
    }

    @NonNull
    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> mapObject = new HashMap<>();
        mapObject.put(FIELD_ID, mId);
        mapObject.put(FIELD_OBJECT, VALUE_OBJECT);
        mapObject.put(FIELD_ACCOUNT_BALANCE, mAccountBalance);
        mapObject.put(FIELD_BUSINESS_VAT_ID, mBusinessVatId);
        mapObject.put(FIELD_CREATED, mCreated);
        mapObject.put(FIELD_DEFAULT_SOURCE, mDefaultSource);
        mapObject.put(FIELD_DELINQUENT, mDelinquent);
        mapObject.put(FIELD_DISCOUNT, mDiscount);
        mapObject.put(FIELD_EMAIL, mEmail);
        mapObject.put(FIELD_LIVEMODE, mLivemode);
        mapObject.put(FIELD_METADATA, mMetadata);
        StripeJsonModel.putStripeJsonModelMapIfNotNull(
                mapObject,
                FIELD_SHIPPING,
                mShippingInformation);

        StripeJsonModel.putStripeJsonModelMapIfNotNull(
                mapObject,
                FIELD_SOURCES,
                mCustomerSources);

        StripeNetworkUtils.removeNullAndEmptyParams(mapObject);
        return mapObject;
    }

    @Nullable
    public static Customer fromString(String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            return fromJson(jsonObject);
        } catch (JSONException ignored) {
            return null;
        }
    }

    @Nullable
    public static Customer fromJson(JSONObject jsonObject) {
        String objectType = optString(jsonObject, FIELD_OBJECT);
        if (!VALUE_OBJECT.equals(objectType)) {
            return null;
        }
        Customer customer = new Customer();
        customer.mId = optString(jsonObject, FIELD_ID);
        customer.mAccountBalance = optInteger(jsonObject, FIELD_ACCOUNT_BALANCE);
        customer.mBusinessVatId = optString(jsonObject, FIELD_BUSINESS_VAT_ID);
        customer.mCreated = optLong(jsonObject, FIELD_CREATED);
        customer.mCurrency = optCurrency(jsonObject, FIELD_CURRENCY);
        customer.mDefaultSource = optString(jsonObject, FIELD_DEFAULT_SOURCE);
        customer.mDelinquent = optBoolean(jsonObject, FIELD_DELINQUENT);
        customer.mDiscount = optMap(jsonObject, FIELD_DISCOUNT);
        customer.mEmail = optString(jsonObject, FIELD_EMAIL);
        customer.mLivemode = optBoolean(jsonObject, FIELD_LIVEMODE);
        customer.mMetadata = optHash(jsonObject, FIELD_METADATA);
        customer.mShippingInformation =
                ShippingInformation.fromJson(jsonObject.optJSONObject(FIELD_SHIPPING));
        customer.mCustomerSources =
                CustomerSources.fromJson(jsonObject.optJSONObject(FIELD_SOURCES));
        return customer;
    }
}
