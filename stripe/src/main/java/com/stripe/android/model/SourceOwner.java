package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.utils.ObjectUtils;

import org.json.JSONException;
import org.json.JSONObject;

import static com.stripe.android.model.StripeJsonUtils.optString;

/**
 * Model for a <a href="https://stripe.com/docs/api#source_object-owner">owner</a> object
 * in the Source api.
 */
public final class SourceOwner extends StripeModel {

    private static final String VERIFIED = "verified_";
    private static final String FIELD_ADDRESS = "address";
    private static final String FIELD_EMAIL = "email";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_PHONE = "phone";
    private static final String FIELD_VERIFIED_ADDRESS = VERIFIED + FIELD_ADDRESS;
    private static final String FIELD_VERIFIED_EMAIL = VERIFIED + FIELD_EMAIL;
    private static final String FIELD_VERIFIED_NAME = VERIFIED + FIELD_NAME;
    private static final String FIELD_VERIFIED_PHONE = VERIFIED + FIELD_PHONE;

    @Nullable private final Address mAddress;
    @Nullable private final String mEmail;
    @Nullable private final String mName;
    @Nullable private final String mPhone;
    @Nullable private final Address mVerifiedAddress;
    @Nullable private final String mVerifiedEmail;
    @Nullable private final String mVerifiedName;
    @Nullable private final String mVerifiedPhone;

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
