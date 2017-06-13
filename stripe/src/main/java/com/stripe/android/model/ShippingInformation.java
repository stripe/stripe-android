package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.util.StripeNetworkUtils;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static com.stripe.android.util.StripeJsonUtils.optString;
import static com.stripe.android.util.StripeJsonUtils.putStringIfNotNull;

/**
 * Model representing a shipping address object
 */
public class ShippingInformation extends StripeJsonModel {

    private static final String FIELD_ADDRESS = "address";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_PHONE = "phone";

    private @Nullable SourceAddress mAddress;
    private @Nullable String mName;
    private @Nullable String mPhone;

    @Nullable
    public SourceAddress getAddress() {
        return mAddress;
    }

    @Nullable
    public String getName() {
        return mName;
    }

    @Nullable
    public String getPhone() {
        return mPhone;
    }

    @Nullable
    public static ShippingInformation fromJson(@Nullable JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }

        ShippingInformation shippingInformation = new ShippingInformation();
        shippingInformation.mName = optString(jsonObject, FIELD_NAME);
        shippingInformation.mPhone = optString(jsonObject, FIELD_PHONE);
        shippingInformation.mAddress =
                SourceAddress.fromJson(jsonObject.optJSONObject(FIELD_ADDRESS));
        return shippingInformation;
    }

    @NonNull
    @Override
    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        putStringIfNotNull(jsonObject, FIELD_NAME, mName);
        putStringIfNotNull(jsonObject, FIELD_PHONE, mPhone);
        putStripeJsonModelIfNotNull(jsonObject, FIELD_ADDRESS, mAddress);
        return jsonObject;
    }

    @NonNull
    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(FIELD_NAME, mName);
        map.put(FIELD_PHONE, mPhone);
        putStripeJsonModelMapIfNotNull(map, FIELD_ADDRESS, mAddress);
        StripeNetworkUtils.removeNullAndEmptyParams(map);
        return map;
    }
}
