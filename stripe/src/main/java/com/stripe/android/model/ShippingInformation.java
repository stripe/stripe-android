package com.stripe.android.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.StripeNetworkUtils;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static com.stripe.android.model.StripeJsonUtils.optString;
import static com.stripe.android.model.StripeJsonUtils.putStringIfNotNull;

/**
 * Model representing a shipping address object
 */
public class ShippingInformation extends StripeJsonModel implements Parcelable {

    private static final String FIELD_ADDRESS = "address";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_PHONE = "phone";

    private @Nullable Address mAddress;
    private @Nullable String mName;
    private @Nullable String mPhone;

    public ShippingInformation() {}

    public ShippingInformation(@Nullable Address address, @Nullable String name, @Nullable String phone) {
        mAddress = address;
        mName = name;
        mPhone = phone;
    }

    private ShippingInformation(Parcel in) {
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

        ShippingInformation shippingInformation = new ShippingInformation();
        shippingInformation.mName = optString(jsonObject, FIELD_NAME);
        shippingInformation.mPhone = optString(jsonObject, FIELD_PHONE);
        shippingInformation.mAddress =
                Address.fromJson(jsonObject.optJSONObject(FIELD_ADDRESS));
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

    static final Parcelable.Creator<ShippingInformation> CREATOR
            = new Parcelable.Creator<ShippingInformation>() {

        @Override
        public ShippingInformation createFromParcel(Parcel in) {
            return new ShippingInformation(in);
        }

        @Override
        public ShippingInformation[] newArray(int size) {
            return new ShippingInformation[size];
        }
    };

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeParcelable(mAddress, flags);
        out.writeString(mName);
        out.writeString(mPhone);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
