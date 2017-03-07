package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static com.stripe.android.util.StripeJsonUtils.optString;
import static com.stripe.android.util.StripeJsonUtils.putStringIfNotNull;

/**
 * Model for an owner <a href="https://stripe.com/docs/api#source_object-owner-address">address</a>
 * object in the Source api.
 */
public class SourceAddress extends StripeJsonModel {

    private static final String FIELD_CITY = "city";
    private static final String FIELD_COUNTRY = "country";
    private static final String FIELD_LINE_1 = "line1";
    private static final String FIELD_LINE_2 = "line2";
    private static final String FIELD_POSTAL_CODE = "postal_code";
    private static final String FIELD_STATE = "state";

    private String mCity;
    private String mCountry;
    private String mLine1;
    private String mLine2;
    private String mPostalCode;
    private String mState;

    SourceAddress(
            String city,
            String country,
            String line1,
            String line2,
            String postalCode,
            String state) {
        mCity = city;
        mCountry = country;
        mLine1 = line1;
        mLine2 = line2;
        mPostalCode = postalCode;
        mState = state;
    }

    public String getCity() {
        return mCity;
    }

    public void setCity(String city) {
        mCity = city;
    }

    public String getCountry() {
        return mCountry;
    }

    public void setCountry(String country) {
        mCountry = country;
    }

    public String getLine1() {
        return mLine1;
    }

    public void setLine1(String line1) {
        mLine1 = line1;
    }

    public String getLine2() {
        return mLine2;
    }

    public void setLine2(String line2) {
        mLine2 = line2;
    }

    public String getPostalCode() {
        return mPostalCode;
    }

    public void setPostalCode(String postalCode) {
        mPostalCode = postalCode;
    }

    public String getState() {
        return mState;
    }

    public void setState(String state) {
        mState = state;
    }

    @NonNull
    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> hashMap = new HashMap<>();
        hashMap.put(FIELD_CITY, mCity);
        hashMap.put(FIELD_COUNTRY, mCountry);
        hashMap.put(FIELD_LINE_1, mLine1);
        hashMap.put(FIELD_LINE_2, mLine2);
        hashMap.put(FIELD_POSTAL_CODE, mPostalCode);
        hashMap.put(FIELD_STATE, mState);
        return hashMap;
    }

    @NonNull
    @Override
    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        putStringIfNotNull(jsonObject, FIELD_CITY, mCity);
        putStringIfNotNull(jsonObject, FIELD_COUNTRY, mCountry);
        putStringIfNotNull(jsonObject, FIELD_LINE_1, mLine1);
        putStringIfNotNull(jsonObject, FIELD_LINE_2, mLine2);
        putStringIfNotNull(jsonObject, FIELD_POSTAL_CODE, mPostalCode);
        putStringIfNotNull(jsonObject, FIELD_STATE, mState);
        return jsonObject;
    }

    @Nullable
    public static SourceAddress fromString(@Nullable String jsonString) {
        try {
            return fromJson(new JSONObject(jsonString));
        } catch (JSONException ignored) {
            return null;
        }
    }

    @Nullable
    public static SourceAddress fromJson(@Nullable JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }

        String city = optString(jsonObject, FIELD_CITY);
        String country = optString(jsonObject, FIELD_COUNTRY);
        String line1 = optString(jsonObject, FIELD_LINE_1);
        String line2 = optString(jsonObject, FIELD_LINE_2);
        String postalCode = optString(jsonObject, FIELD_POSTAL_CODE);
        String state = optString(jsonObject, FIELD_STATE);

        return new SourceAddress(city, country, line1, line2, postalCode, state);
    }
}
