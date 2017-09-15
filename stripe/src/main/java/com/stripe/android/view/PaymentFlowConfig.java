package com.stripe.android.view;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.stripe.android.model.Address;
import com.stripe.android.model.ShippingInformation;

import java.util.ArrayList;
import java.util.List;

public class PaymentFlowConfig implements Parcelable {

    @NonNull private List<String> mHiddenAddressFields;
    @NonNull private List<String> mOptionalAddressFields;
    @NonNull private ShippingInformation mShippingInformation;
    private boolean mHideAddressScreen;
    private boolean mHideShippingScreen;

    public PaymentFlowConfig(
            @NonNull List<String> hiddenAddressFields,
            @NonNull List<String> optionalAddressFields,
            @NonNull ShippingInformation shippingInformation,
            boolean hideAddressScreen,
            boolean hideShippingScreen) {
        mHiddenAddressFields = hiddenAddressFields;
        mOptionalAddressFields = optionalAddressFields;
        mShippingInformation = shippingInformation;
        mHideAddressScreen = hideAddressScreen;
        mHideShippingScreen = hideShippingScreen;
    }

    private PaymentFlowConfig(Parcel in) {
        mHiddenAddressFields = new ArrayList<>();
        in.readStringList(mHiddenAddressFields);
        mOptionalAddressFields = new ArrayList<>();
        in.readStringList(mOptionalAddressFields);
        mShippingInformation = in.readParcelable(Address.class.getClassLoader());
        mHideAddressScreen = in.readInt() == 1;
        mHideShippingScreen = in.readInt() == 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PaymentFlowConfig that = (PaymentFlowConfig) o;

        if (isHideAddressScreen() != that.isHideAddressScreen()) return false;
        if (isHideShippingScreen() != that.isHideShippingScreen()) return false;
        if (!getHiddenAddressFields().equals(that.getHiddenAddressFields())) return false;
        if (!getOptionalAddressFields().equals(that.getOptionalAddressFields())) return false;
        return getPrepopulatedShippingInfo().equals(that.getPrepopulatedShippingInfo());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeStringList(mHiddenAddressFields);
        parcel.writeStringList(mOptionalAddressFields);
        parcel.writeParcelable(mShippingInformation, flags);
        parcel.writeInt(mHideAddressScreen ? 1 : 0);
        parcel.writeInt(mHideShippingScreen? 1: 0);
    }

    @NonNull List<String> getHiddenAddressFields() {
        return mHiddenAddressFields;
    }

    @NonNull List<String> getOptionalAddressFields() {
        return mOptionalAddressFields;
    }

    @NonNull
    ShippingInformation getPrepopulatedShippingInfo() {
        return mShippingInformation;
    }

    boolean isHideAddressScreen() {
        return mHideAddressScreen;
    }

    boolean isHideShippingScreen() {
        return mHideShippingScreen;
    }

    static final Parcelable.Creator<PaymentFlowConfig> CREATOR
            = new Parcelable.Creator<PaymentFlowConfig>() {

        @Override
        public PaymentFlowConfig createFromParcel(Parcel in) {
            return new PaymentFlowConfig(in);
        }

        @Override
        public PaymentFlowConfig[] newArray(int size) {
            return new PaymentFlowConfig[size];
        }
    };
}
