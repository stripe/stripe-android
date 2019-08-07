package com.stripe.android.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.StripeNetworkUtils;
import com.stripe.android.utils.ObjectUtils;

import org.json.JSONObject;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

import static com.stripe.android.model.StripeJsonUtils.optString;

/**
 * Model representing a shipping address object
 */
public final class ShippingInformation
        extends StripeModel
        implements StripeParamsModel, Parcelable {
    public static final Creator<ShippingInformation> CREATOR = new Creator<ShippingInformation>() {
        @Override
        public ShippingInformation createFromParcel(Parcel source) {
            return new ShippingInformation(source);
        }

        @Override
        public ShippingInformation[] newArray(int size) {
            return new ShippingInformation[size];
        }
    };

    private static final String FIELD_ADDRESS = "address";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_PHONE = "phone";

    @Nullable private final Address mAddress;
    @Nullable private final String mName;
    @Nullable private final String mPhone;

    public ShippingInformation(@Nullable Address address, @Nullable String name,
                               @Nullable String phone) {
        mAddress = address;
        mName = name;
        mPhone = phone;
    }

    protected ShippingInformation(@NonNull Parcel in) {
        mAddress = in.readParcelable(Address.class.getClassLoader());
        mName = in.readString();
        mPhone = in.readString();
    }

    @Nullable
    public Address getAddress() {
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

        return new ShippingInformation(
                Address.fromJson(jsonObject.optJSONObject(FIELD_ADDRESS)),
                optString(jsonObject, FIELD_NAME),
                optString(jsonObject, FIELD_PHONE));
    }

    @NonNull
    @Override
    public Map<String, Object> toParamMap() {
        final AbstractMap<String, Object> map = new HashMap<>();
        map.put(FIELD_NAME, mName);
        map.put(FIELD_PHONE, mPhone);
        if (mAddress != null) {
            map.put(FIELD_ADDRESS, mAddress.toParamMap());
        }
        StripeNetworkUtils.removeNullAndEmptyParams(map);
        return map;
    }

    /************** Parcelable *********************/
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeParcelable(mAddress, flags);
        dest.writeString(mName);
        dest.writeString(mPhone);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj
                || (obj instanceof ShippingInformation && typedEquals((ShippingInformation) obj));
    }

    private boolean typedEquals(@NonNull ShippingInformation shippingInformation) {
        return ObjectUtils.equals(mAddress, shippingInformation.mAddress)
                && ObjectUtils.equals(mName, shippingInformation.mName)
                && ObjectUtils.equals(mPhone, shippingInformation.mPhone);
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hash(mAddress, mName, mPhone);
    }
}
