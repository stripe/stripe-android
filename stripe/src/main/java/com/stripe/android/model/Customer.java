package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.util.StripeNetworkUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static com.stripe.android.util.StripeJsonUtils.optString;
import static com.stripe.android.util.StripeJsonUtils.putStringIfNotNull;

/**
 * Model for a Stripe Customer object
 */
public class Customer extends StripeJsonModel {

    private static final String FIELD_ID = "id";
    private static final String FIELD_OBJECT = "object";
    private static final String FIELD_DEFAULT_SOURCE = "default_source";
    private static final String FIELD_SHIPPING = "shipping";
    private static final String FIELD_SOURCES = "sources";

    private static final String VALUE_OBJECT = "customer";

    private String mId;
    private String mDefaultSource;
    private ShippingInformation mShippingInformation;
    private CustomerSources mCustomerSources;

    private Customer() { }

    public String getId() {
        return mId;
    }

    public String getDefaultSource() {
        return mDefaultSource;
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
        putStringIfNotNull(jsonObject, FIELD_DEFAULT_SOURCE, mDefaultSource);
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
        mapObject.put(FIELD_DEFAULT_SOURCE, mDefaultSource);
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
        customer.mDefaultSource = optString(jsonObject, FIELD_DEFAULT_SOURCE);
        customer.mShippingInformation =
                ShippingInformation.fromJson(jsonObject.optJSONObject(FIELD_SHIPPING));
        customer.mCustomerSources =
                CustomerSources.fromJson(jsonObject.optJSONObject(FIELD_SOURCES));
        return customer;
    }
}
