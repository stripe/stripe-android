package com.stripe.android.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.stripe.android.utils.ObjectUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.stripe.android.model.StripeJsonUtils.optString;
import static com.stripe.android.model.StripeJsonUtils.putStringIfNotNull;

/**
 * Model for an owner <a href="https://stripe.com/docs/api#source_object-owner-address">address</a>
 * object in the Source api.
 */
public class Address extends StripeJsonModel implements Parcelable {

    @IntDef({
            RequiredBillingAddressFields.NONE,
            RequiredBillingAddressFields.ZIP,
            RequiredBillingAddressFields.FULL,
            RequiredBillingAddressFields.NAME})
    @Retention(RetentionPolicy.SOURCE)
    public @interface RequiredBillingAddressFields {
        int NONE = 0;
        int ZIP = 1;
        int FULL = 2;
        int NAME = 3;
    }

    private static final String FIELD_CITY = "city";
    /* 2 Character Country Code */
    private static final String FIELD_COUNTRY = "country";
    private static final String FIELD_LINE_1 = "line1";
    private static final String FIELD_LINE_2 = "line2";
    private static final String FIELD_POSTAL_CODE = "postal_code";
    private static final String FIELD_STATE = "state";

    @Nullable private String mCity;
    @Nullable private String mCountry;
    @Nullable private String mLine1;
    @Nullable private String mLine2;
    @Nullable private String mPostalCode;
    @Nullable private String mState;

    private Address(
            @Nullable String city,
            @Nullable String country,
            @Nullable String line1,
            @Nullable String line2,
            @Nullable String postalCode,
            @Nullable String state) {
        mCity = city;
        mCountry = country;
        mLine1 = line1;
        mLine2 = line2;
        mPostalCode = postalCode;
        mState = state;
    }

    private Address(@NonNull Builder addressBuilder) {
        this(addressBuilder.mCity, addressBuilder.mCountry, addressBuilder.mLine1,
                addressBuilder.mLine2, addressBuilder.mPostalCode, addressBuilder.mState);
    }

    protected Address(@NonNull Parcel in) {
        mCity = in.readString();
        mCountry = in.readString();
        mLine1 = in.readString();
        mLine2 = in.readString();
        mPostalCode = in.readString();
        mState = in.readString();
    }

    @Nullable
    public String getCity() {
        return mCity;
    }

    @Deprecated
    public void setCity(String city) {
        mCity = city;
    }

    @Nullable
    public String getCountry() {
        return mCountry;
    }

    @Deprecated
    public void setCountry(String country) {
        mCountry = country;
    }

    @Nullable
    public String getLine1() {
        return mLine1;
    }

    @Deprecated
    public void setLine1(String line1) {
        mLine1 = line1;
    }

    @Nullable
    public String getLine2() {
        return mLine2;
    }

    @Deprecated
    public void setLine2(String line2) {
        mLine2 = line2;
    }

    @Nullable
    public String getPostalCode() {
        return mPostalCode;
    }

    @Deprecated
    public void setPostalCode(String postalCode) {
        mPostalCode = postalCode;
    }

    @Nullable
    public String getState() {
        return mState;
    }

    @Deprecated
    public void setState(String state) {
        mState = state;
    }

    @NonNull
    @Override
    public Map<String, Object> toMap() {
        final Map<String, Object> map = new HashMap<>();
        map.put(FIELD_CITY, mCity);
        map.put(FIELD_COUNTRY, mCountry);
        map.put(FIELD_LINE_1, mLine1);
        map.put(FIELD_LINE_2, mLine2);
        map.put(FIELD_POSTAL_CODE, mPostalCode);
        map.put(FIELD_STATE, mState);
        return map;
    }

    @NonNull
    @Override
    public JSONObject toJson() {
        final JSONObject jsonObject = new JSONObject();
        putStringIfNotNull(jsonObject, FIELD_CITY, mCity);
        putStringIfNotNull(jsonObject, FIELD_COUNTRY, mCountry);
        putStringIfNotNull(jsonObject, FIELD_LINE_1, mLine1);
        putStringIfNotNull(jsonObject, FIELD_LINE_2, mLine2);
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

        final String city = optString(jsonObject, FIELD_CITY);
        final String country = optString(jsonObject, FIELD_COUNTRY);
        final String line1 = optString(jsonObject, FIELD_LINE_1);
        final String line2 = optString(jsonObject, FIELD_LINE_2);
        final String postalCode = optString(jsonObject, FIELD_POSTAL_CODE);
        final String state = optString(jsonObject, FIELD_STATE);
        return new Address(city, country, line1, line2, postalCode, state);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof Address && typedEquals((Address) obj));
    }

    private boolean typedEquals(@NonNull Address obj) {
        return ObjectUtils.equals(mCity, obj.mCity)
                && ObjectUtils.equals(mCountry, obj.mCountry)
                && ObjectUtils.equals(mLine1, obj.mLine1)
                && ObjectUtils.equals(mLine2, obj.mLine2)
                && ObjectUtils.equals(mPostalCode, obj.mPostalCode)
                && ObjectUtils.equals(mState, obj.mState);
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hash(mCity, mCountry, mLine1, mLine2, mPostalCode, mState);
    }

    /************** Parcelable *********************/
    public static final Parcelable.Creator<Address> CREATOR
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
        private String mPostalCode;
        private String mState;

        @NonNull
        public Builder setCity(@Nullable String city) {
            mCity = city;
            return this;
        }

        @NonNull
        public Builder setCountry(@NonNull String country) {
            mCountry = country.toUpperCase(Locale.ROOT);
            return this;
        }

        @NonNull
        public Builder setLine1(@Nullable String line1) {
            mLine1 = line1;
            return this;
        }

        @NonNull
        public Builder setLine2(@Nullable String line2) {
            mLine2 = line2;
            return this;
        }

        @NonNull
        public Builder setPostalCode(@Nullable String postalCode) {
            mPostalCode = postalCode;
            return this;
        }

        @NonNull
        public Builder setState(@Nullable String state) {
            mState = state;
            return this;
        }

        @NonNull
        public Address build() {
            return new Address(this);
        }

    }
}
