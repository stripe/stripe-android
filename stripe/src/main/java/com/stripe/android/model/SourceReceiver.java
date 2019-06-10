package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.StripeTextUtils;
import com.stripe.android.utils.ObjectUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Model for a
 * <a href="https://stripe.com/docs/api/sources/object#source_object-receiver">receiver</a> object
 * in the Sources API. Present if the {@link Source} is a receiver.
 */
public final class SourceReceiver extends StripeModel {

    private static final String FIELD_ADDRESS = "address";
    private static final String FIELD_AMOUNT_CHARGED = "amount_charged";
    private static final String FIELD_AMOUNT_RECEIVED = "amount_received";
    private static final String FIELD_AMOUNT_RETURNED = "amount_returned";

    // This is not to be confused with the Address object
    @Nullable private final String mAddress;
    private final long mAmountCharged;
    private final long mAmountReceived;
    private final long mAmountReturned;

    private SourceReceiver(@Nullable String address,
                           long amountCharged,
                           long amountReceived,
                           long amountReturned) {
        mAddress = address;
        mAmountCharged = amountCharged;
        mAmountReceived = amountReceived;
        mAmountReturned = amountReturned;
    }

    @Nullable
    public String getAddress() {
        return mAddress;
    }

    public long getAmountCharged() {
        return mAmountCharged;
    }

    public long getAmountReceived() {
        return mAmountReceived;
    }

    public long getAmountReturned() {
        return mAmountReturned;
    }

    @NonNull
    @Override
    public Map<String, Object> toMap() {
        final AbstractMap<String, Object> map = new HashMap<>();
        if (!StripeTextUtils.isBlank(mAddress)) {
            map.put(FIELD_ADDRESS, mAddress);
        }
        map.put(FIELD_ADDRESS, mAddress);
        map.put(FIELD_AMOUNT_CHARGED, mAmountCharged);
        map.put(FIELD_AMOUNT_RECEIVED, mAmountReceived);
        map.put(FIELD_AMOUNT_RETURNED, mAmountReturned);
        return map;
    }

    @Nullable
    public static SourceReceiver fromString(@Nullable String jsonString) {
        try {
            return fromJson(new JSONObject(jsonString));
        } catch (JSONException ignored) {
            return null;
        }
    }

    @Nullable
    public static SourceReceiver fromJson(@Nullable JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }

        return new SourceReceiver(
                StripeJsonUtils.optString(jsonObject, FIELD_ADDRESS),
                jsonObject.optLong(FIELD_AMOUNT_CHARGED),
                jsonObject.optLong(FIELD_AMOUNT_RECEIVED),
                jsonObject.optLong(FIELD_AMOUNT_RETURNED));
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof SourceReceiver && typedEquals((SourceReceiver) obj));
    }

    private boolean typedEquals(@NonNull SourceReceiver sourceReceiver) {
        return ObjectUtils.equals(mAddress, sourceReceiver.mAddress)
                && mAmountCharged == sourceReceiver.mAmountCharged
                && mAmountReceived == sourceReceiver.mAmountReceived
                && mAmountReturned == sourceReceiver.mAmountReturned;
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hash(mAddress, mAmountCharged, mAmountReceived, mAmountReturned);
    }
}
