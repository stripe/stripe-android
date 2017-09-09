package com.stripe.android.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.Map;

import static com.stripe.android.model.StripeJsonUtils.optString;
import static com.stripe.android.model.StripeJsonUtils.putStringIfNotNull;

/**
 * Model for an owner <a href="https://stripe.com/docs/api#source_object-owner-address">address</a>
 * object in the Source api.
 */
public class Address extends StripeJsonModel implements Parcelable{

    @IntDef({
            RequiredBillingAddressFields.NONE,
            RequiredBillingAddressFields.ZIP,
            RequiredBillingAddressFields.FULL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface RequiredBillingAddressFields {
        int NONE = 0;
        int ZIP = 1;
        int FULL = 2;
    }

    private Address(Parcel in) {
        mCity = in.readString();
        mCountry = in.readString();
        mLine1 = in.readString();
        mLine2 = in.readString();
        mName = in.readString();
        mPhoneNumber = in.readString();
        mPostalCode = in.readString();
        mState = in.readString();
    }

    private static final String FIELD_CITY = "city";
    /* 2 Character Country Code */
    private static final String FIELD_COUNTRY = "country";
    private static final String FIELD_LINE_1 = "line1";
    private static final String FIELD_LINE_2 = "line2";
    private static final String FIELD_POSTAL_CODE = "postal_code";
    private static final String FIELD_STATE = "state";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_PHONE_NUMBER = "phone_number";

    private String mCity;
    private String mCountry;
    private String mLine1;
    private String mLine2;
    private String mName;
    private String mPhoneNumber;
    private String mPostalCode;
    private String mState;

    Address(
            String city,
            String country,
            String line1,
            String line2,
            String name,
            String phoneNumber,
            String postalCode,
            String state) {
        mCity = city;
        mCountry = country;
        mLine1 = line1;
        mLine2 = line2;
        mName = name;
        mPhoneNumber = phoneNumber;
        mPostalCode = postalCode;
        mState = state;
    }

    Address(Builder addressBuilder) {
        mCity = addressBuilder.mCity;
        mCountry = addressBuilder.mCountry;
        mLine1 = addressBuilder.mLine1;
        mLine2 = addressBuilder.mLine2;
        mName = addressBuilder.mName;
        mPhoneNumber = addressBuilder.mPhoneNumber;
        mPostalCode = addressBuilder.mPostalCode;
        mState = addressBuilder.mState;
    }

    public String getCity() {
        return mCity;
    }

    @Deprecated
    public void setCity(String city) {
        mCity = city;
    }

    public String getCountry() {
        return mCountry;
    }

    @Deprecated
    public void setCountry(String country) {
        mCountry = country;
    }

    public String getLine1() {
        return mLine1;
    }

    @Deprecated
    public void setLine1(String line1) {
        mLine1 = line1;
    }

    public String getLine2() {
        return mLine2;
    }

    @Deprecated
    public void setLine2(String line2) {
        mLine2 = line2;
    }

    public String getPostalCode() {
        return mPostalCode;
    }

    @Deprecated
    public void setPostalCode(String postalCode) {
        mPostalCode = postalCode;
    }

    public String getState() {
        return mState;
    }

    @Deprecated
    public void setState(String state) {
        mState = state;
    }

    public String getName() {
        return mName;
    }

    public String getPhoneNumber() {
        return mPhoneNumber;
    }

    @NonNull
    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> hashMap = new HashMap<>();
        hashMap.put(FIELD_CITY, mCity);
        hashMap.put(FIELD_COUNTRY, mCountry);
        hashMap.put(FIELD_LINE_1, mLine1);
        hashMap.put(FIELD_LINE_2, mLine2);
        hashMap.put(FIELD_NAME, mName);
        hashMap.put(FIELD_PHONE_NUMBER, mPhoneNumber);
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
        putStringIfNotNull(jsonObject, FIELD_NAME, mName);
        putStringIfNotNull(jsonObject, FIELD_PHONE_NUMBER, mPhoneNumber);
        putStringIfNotNull(jsonObject, FIELD_POSTAL_CODE, mPostalCode);
        putStringIfNotNull(jsonObject, FIELD_STATE, mState);
        return jsonObject;
    }

    @Nullable
    public static Address fromString(@Nullable String jsonString) {
        try {
            return fromJson(new JSONObject(jsonString));
        } catch (JSONException ignored) {
            return null;
        }
    }

    @Nullable
    public static Address fromJson(@Nullable JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }

        String city = optString(jsonObject, FIELD_CITY);
        String country = optString(jsonObject, FIELD_COUNTRY);
        String line1 = optString(jsonObject, FIELD_LINE_1);
        String line2 = optString(jsonObject, FIELD_LINE_2);
        String name = optString(jsonObject, FIELD_NAME);
        String phoneNumber = optString(jsonObject, FIELD_PHONE_NUMBER);
        String postalCode = optString(jsonObject, FIELD_POSTAL_CODE);
        String state = optString(jsonObject, FIELD_STATE);

        return new Address(city, country, line1, line2, name, phoneNumber, postalCode, state);
    }

    static final Parcelable.Creator<Address> CREATOR
            = new Parcelable.Creator<Address>() {

        @Override
        public Address createFromParcel(Parcel in) {
            return new Address(in);
        }

        @Override
        public Address[] newArray(int size) {
            return new Address[size];
        }
    };

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(mCity);
        out.writeString(mCountry);
        out.writeString(mLine1);
        out.writeString(mLine2);
        out.writeString(mName);
        out.writeString(mPhoneNumber);
        out.writeString(mPostalCode);
        out.writeString(mState);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static class Builder {
        private String mCity;
        private String mCountry;
        private String mLine1;
        private String mLine2;
        private String mName;
        private String mPhoneNumber;
        private String mPostalCode;
        private String mState;

        public Builder setCity(String city) {
            mCity = city;
            return this;
        }

        public Builder setCountry(@NonNull String country) {
            mCountry = country.toUpperCase();
            return this;
        }

        public Builder setLine1(String line1) {
            mLine1 = line1;
            return this;
        }

        public Builder setLine2(String line2) {
            mLine2 = line2;
            return this;
        }

        public Builder setName(String name) {
            mName = name;
            return this;
        }

        public Builder setPhoneNumber(String phoneNumber) {
            mPhoneNumber = phoneNumber;
            return this;
        }

        public Builder setPostalCode(String postalCode) {
            mPostalCode = postalCode;
            return this;
        }

        public Builder setState(String state) {
            mState = state;
            return this;
        }

        public Address build() {
            return new Address(this);
        }

    }
}
