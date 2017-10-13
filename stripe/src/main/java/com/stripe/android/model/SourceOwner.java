package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static com.stripe.android.StripeNetworkUtils.removeNullAndEmptyParams;
import static com.stripe.android.model.StripeJsonUtils.optString;
import static com.stripe.android.model.StripeJsonUtils.putStringIfNotNull;

/**
 * Model for a <a href="https://stripe.com/docs/api#source_object-owner">owner</a> object
 * in the Source api.
 */
public class SourceOwner extends StripeJsonModel {

    private static final String VERIFIED = "verified_";
    private static final String FIELD_ADDRESS = "address";
    private static final String FIELD_EMAIL = "email";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_PHONE = "phone";
    private static final String FIELD_VERIFIED_ADDRESS = VERIFIED + FIELD_ADDRESS;
    private static final String FIELD_VERIFIED_EMAIL = VERIFIED + FIELD_EMAIL;
    private static final String FIELD_VERIFIED_NAME = VERIFIED + FIELD_NAME;
    private static final String FIELD_VERIFIED_PHONE = VERIFIED + FIELD_PHONE;

    private Address mAddress;
    private String mEmail;
    private String mName;
    private String mPhone;
    private Address mVerifiedAddress;
    private String mVerifiedEmail;
    private String mVerifiedName;
    private String mVerifiedPhone;

    SourceOwner(
            Address address,
            String email,
            String name,
            String phone,
            Address verifiedAddress,
            String verifiedEmail,
            String verifiedName,
            String verifiedPhone) {
        mAddress = address;
        mEmail = email;
        mName = name;
        mPhone = phone;
        mVerifiedAddress = verifiedAddress;
        mVerifiedEmail = verifiedEmail;
        mVerifiedName = verifiedName;
        mVerifiedPhone = verifiedPhone;
    }

    public Address getAddress() {
        return mAddress;
    }

    public String getEmail() {
        return mEmail;
    }

    public String getName() {
        return mName;
    }

    public String getPhone() {
        return mPhone;
    }

    public Address getVerifiedAddress() {
        return mVerifiedAddress;
    }

    public String getVerifiedEmail() {
        return mVerifiedEmail;
    }

    public String getVerifiedName() {
        return mVerifiedName;
    }

    public String getVerifiedPhone() {
        return mVerifiedPhone;
    }

    void setAddress(Address address) {
        mAddress = address;
    }

    void setEmail(String email) {
        mEmail = email;
    }

    void setName(String name) {
        mName = name;
    }

    void setPhone(String phone) {
        mPhone = phone;
    }

    void setVerifiedAddress(Address verifiedAddress) {
        mVerifiedAddress = verifiedAddress;
    }

    void setVerifiedEmail(String verifiedEmail) {
        mVerifiedEmail = verifiedEmail;
    }

    void setVerifiedName(String verifiedName) {
        mVerifiedName = verifiedName;
    }

    void setVerifiedPhone(String verifiedPhone) {
        mVerifiedPhone = verifiedPhone;
    }

    @NonNull
    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> hashMap = new HashMap<>();
        if (mAddress != null) {
            hashMap.put(FIELD_ADDRESS, mAddress.toMap());
        }
        hashMap.put(FIELD_EMAIL, mEmail);
        hashMap.put(FIELD_NAME, mName);
        hashMap.put(FIELD_PHONE, mPhone);
        if (mVerifiedAddress != null) {
            hashMap.put(FIELD_VERIFIED_ADDRESS, mVerifiedAddress.toMap());
        }
        hashMap.put(FIELD_VERIFIED_EMAIL, mVerifiedEmail);
        hashMap.put(FIELD_VERIFIED_NAME, mVerifiedName);
        hashMap.put(FIELD_VERIFIED_PHONE, mVerifiedPhone);
        removeNullAndEmptyParams(hashMap);
        return hashMap;
    }

    @NonNull
    @Override
    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        JSONObject jsonAddressObject = mAddress == null ? null : mAddress.toJson();
        JSONObject jsonVerifiedAddressObject = mVerifiedAddress == null
                ? null
                : mVerifiedAddress.toJson();
        try {
            if (jsonAddressObject != null && jsonAddressObject.length() > 0) {
                jsonObject.put(FIELD_ADDRESS, jsonAddressObject);
            }
            putStringIfNotNull(jsonObject, FIELD_EMAIL, mEmail);
            putStringIfNotNull(jsonObject, FIELD_NAME, mName);
            putStringIfNotNull(jsonObject, FIELD_PHONE, mPhone);
            if (jsonVerifiedAddressObject != null && jsonVerifiedAddressObject.length() > 0) {
                jsonObject.put(FIELD_VERIFIED_ADDRESS, jsonVerifiedAddressObject);
            }
            putStringIfNotNull(jsonObject, FIELD_VERIFIED_EMAIL, mVerifiedEmail);
            putStringIfNotNull(jsonObject, FIELD_VERIFIED_NAME, mVerifiedName);
            putStringIfNotNull(jsonObject, FIELD_VERIFIED_PHONE, mVerifiedPhone);
        } catch (JSONException ignored) { }

        return jsonObject;
    }

    @Nullable
    public static SourceOwner fromString(@Nullable String jsonString) {
        try {
            return fromJson(new JSONObject(jsonString));
        } catch (JSONException ignored) {
            return null;
        }
    }

    @Nullable
    public static SourceOwner fromJson(@Nullable JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }

        Address address = null;
        String email;
        String name;
        String phone;
        Address verifiedAddress = null;
        String verifiedEmail;
        String verifiedName;
        String verifiedPhone;

        JSONObject addressObject = jsonObject.optJSONObject(FIELD_ADDRESS);
        if (addressObject != null) {
            address = Address.fromJson(addressObject);
        }
        email = optString(jsonObject, FIELD_EMAIL);
        name = optString(jsonObject, FIELD_NAME);
        phone = optString(jsonObject, FIELD_PHONE);

        JSONObject vAddressObject = jsonObject.optJSONObject(FIELD_VERIFIED_ADDRESS);
        if (vAddressObject != null) {
            verifiedAddress = Address.fromJson(vAddressObject);
        }
        verifiedEmail = optString(jsonObject, FIELD_VERIFIED_EMAIL);
        verifiedName = optString(jsonObject, FIELD_VERIFIED_NAME);
        verifiedPhone = optString(jsonObject, FIELD_VERIFIED_PHONE);

        return new SourceOwner(
                address,
                email,
                name,
                phone,
                verifiedAddress,
                verifiedEmail,
                verifiedName,
                verifiedPhone);
    }
}
