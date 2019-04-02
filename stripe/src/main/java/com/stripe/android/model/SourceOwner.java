package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.utils.ObjectUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.AbstractMap;
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

    @Nullable private Address mAddress;
    @Nullable private String mEmail;
    @Nullable private String mName;
    @Nullable private String mPhone;
    @Nullable private Address mVerifiedAddress;
    @Nullable private String mVerifiedEmail;
    @Nullable private String mVerifiedName;
    @Nullable private String mVerifiedPhone;

    private SourceOwner(
            @Nullable Address address,
            @Nullable String email,
            @Nullable String name,
            @Nullable String phone,
            @Nullable Address verifiedAddress,
            @Nullable String verifiedEmail,
            @Nullable String verifiedName,
            @Nullable String verifiedPhone) {
        mAddress = address;
        mEmail = email;
        mName = name;
        mPhone = phone;
        mVerifiedAddress = verifiedAddress;
        mVerifiedEmail = verifiedEmail;
        mVerifiedName = verifiedName;
        mVerifiedPhone = verifiedPhone;
    }

    @Nullable
    public Address getAddress() {
        return mAddress;
    }

    @Nullable
    public String getEmail() {
        return mEmail;
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
    public Address getVerifiedAddress() {
        return mVerifiedAddress;
    }

    @Nullable
    public String getVerifiedEmail() {
        return mVerifiedEmail;
    }

    @Nullable
    public String getVerifiedName() {
        return mVerifiedName;
    }

    @Nullable
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
        final AbstractMap<String, Object> map = new HashMap<>();
        if (mAddress != null) {
            map.put(FIELD_ADDRESS, mAddress.toMap());
        }
        map.put(FIELD_EMAIL, mEmail);
        map.put(FIELD_NAME, mName);
        map.put(FIELD_PHONE, mPhone);
        if (mVerifiedAddress != null) {
            map.put(FIELD_VERIFIED_ADDRESS, mVerifiedAddress.toMap());
        }
        map.put(FIELD_VERIFIED_EMAIL, mVerifiedEmail);
        map.put(FIELD_VERIFIED_NAME, mVerifiedName);
        map.put(FIELD_VERIFIED_PHONE, mVerifiedPhone);
        removeNullAndEmptyParams(map);
        return map;
    }

    @NonNull
    @Override
    public JSONObject toJson() {
        final JSONObject jsonObject = new JSONObject();
        final JSONObject jsonAddressObject = mAddress == null ? null : mAddress.toJson();
        final JSONObject jsonVerifiedAddressObject = mVerifiedAddress == null
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

        final Address address;
        final JSONObject addressJsonOpt = jsonObject.optJSONObject(FIELD_ADDRESS);
        if (addressJsonOpt != null) {
            address = Address.fromJson(addressJsonOpt);
        } else {
            address = null;
        }

        final String email = optString(jsonObject, FIELD_EMAIL);
        final String name = optString(jsonObject, FIELD_NAME);
        final String phone = optString(jsonObject, FIELD_PHONE);

        final Address verifiedAddress;
        final JSONObject verifiedAddressJsonOpt = jsonObject.optJSONObject(FIELD_VERIFIED_ADDRESS);
        if (verifiedAddressJsonOpt != null) {
            verifiedAddress = Address.fromJson(verifiedAddressJsonOpt);
        } else {
            verifiedAddress = null;
        }

        final String verifiedEmail = optString(jsonObject, FIELD_VERIFIED_EMAIL);
        final String verifiedName = optString(jsonObject, FIELD_VERIFIED_NAME);
        final String verifiedPhone = optString(jsonObject, FIELD_VERIFIED_PHONE);

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

    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof SourceOwner && typedEquals((SourceOwner) obj));
    }

    private boolean typedEquals(@NonNull SourceOwner sourceOwner) {
        return ObjectUtils.equals(mAddress, sourceOwner.mAddress)
                && ObjectUtils.equals(mEmail, sourceOwner.mEmail)
                && ObjectUtils.equals(mName, sourceOwner.mName)
                && ObjectUtils.equals(mPhone, sourceOwner.mPhone)
                && ObjectUtils.equals(mVerifiedAddress, sourceOwner.mVerifiedAddress)
                && ObjectUtils.equals(mVerifiedEmail, sourceOwner.mVerifiedEmail)
                && ObjectUtils.equals(mVerifiedName, sourceOwner.mVerifiedName)
                && ObjectUtils.equals(mVerifiedPhone, sourceOwner.mVerifiedPhone);
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hash(mAddress, mEmail, mName, mPhone, mVerifiedAddress, mVerifiedEmail,
                mVerifiedName, mVerifiedPhone);
    }
}
