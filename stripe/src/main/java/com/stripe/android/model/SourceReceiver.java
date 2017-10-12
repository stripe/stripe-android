package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.StripeTextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Model for a <a href="https://stripe.com/docs/api#source_object-receiver">receiver</a> object in
 * the source api. Present if the {@link Source} is a receiver.
 */
public class SourceReceiver extends StripeJsonModel {

    private static final String FIELD_ADDRESS = "address";
    private static final String FIELD_AMOUNT_CHARGED = "amount_charged";
    private static final String FIELD_AMOUNT_RECEIVED = "amount_received";
    private static final String FIELD_AMOUNT_RETURNED = "amount_returned";

    // This is not to be confused with the Address object
    private String mAddress;
    private long mAmountCharged;
    private long mAmountReceived;
    private long mAmountReturned;

    SourceReceiver(String address,
                   long amountCharged,
                   long amountReceived,
                   long amountReturned) {
        mAddress = address;
        mAmountCharged = amountCharged;
        mAmountReceived = amountReceived;
        mAmountReturned = amountReturned;
    }

    public String getAddress() {
        return mAddress;
    }

    public void setAddress(String address) {
        mAddress = address;
    }

    public long getAmountCharged() {
        return mAmountCharged;
    }

    public void setAmountCharged(long amountCharged) {
        mAmountCharged = amountCharged;
    }

    public long getAmountReceived() {
        return mAmountReceived;
    }

    public void setAmountReceived(long amountReceived) {
        mAmountReceived = amountReceived;
    }

    public long getAmountReturned() {
        return mAmountReturned;
    }

    public void setAmountReturned(long amountReturned) {
        mAmountReturned = amountReturned;
    }

    @NonNull
    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> hashMap = new HashMap<>();
        if (!StripeTextUtils.isBlank(mAddress)) {
            hashMap.put(FIELD_ADDRESS, mAddress);
        }
        hashMap.put(FIELD_ADDRESS, mAddress);
        hashMap.put(FIELD_AMOUNT_CHARGED, mAmountCharged);
        hashMap.put(FIELD_AMOUNT_RECEIVED, mAmountReceived);
        hashMap.put(FIELD_AMOUNT_RETURNED, mAmountReturned);
        return hashMap;
    }

    @NonNull
    @Override
    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        StripeJsonUtils.putStringIfNotNull(jsonObject, FIELD_ADDRESS, mAddress);
        try {
            jsonObject.put(FIELD_AMOUNT_CHARGED, mAmountCharged);
            jsonObject.put(FIELD_AMOUNT_RECEIVED, mAmountReceived);
            jsonObject.put(FIELD_AMOUNT_RETURNED, mAmountReturned);
        } catch (JSONException jsonException) {
            return jsonObject;
        }
        return jsonObject;
    }

    @Nullable
    public static SourceReceiver fromString(@Nullable String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            return fromJson(jsonObject);
        } catch (JSONException ignored) {
            return null;
        }
    }

    @Nullable
    public static SourceReceiver fromJson(@Nullable JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }

        String address = StripeJsonUtils.optString(jsonObject, FIELD_ADDRESS);
        return new SourceReceiver(address,
                jsonObject.optLong(FIELD_AMOUNT_CHARGED),
                jsonObject.optLong(FIELD_AMOUNT_RECEIVED),
                jsonObject.optLong(FIELD_AMOUNT_RETURNED));
    }
}
