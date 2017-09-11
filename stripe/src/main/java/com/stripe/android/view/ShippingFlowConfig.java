package com.stripe.android.view;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.stripe.android.model.Address;

import java.util.ArrayList;
import java.util.List;

class ShippingFlowConfig implements Parcelable {

    private List<String> mHiddenAddressFields;
    private List<String> mOptionalAddressFields;
    private Address mPrepopulatedAddress;
    private boolean mHideAddressScreen;
    private boolean mHideShippingScreen;

    public ShippingFlowConfig(
            @NonNull List<String> hiddenAddressFields,
            @NonNull List<String> optionalAddressFields,
            @NonNull Address prepopulatedAddress,
            boolean hideAddressScreen,
            boolean hideShippingScreen) {
        mHiddenAddressFields = hiddenAddressFields;
        mOptionalAddressFields = optionalAddressFields;
        mPrepopulatedAddress = prepopulatedAddress;
        mHideAddressScreen = hideAddressScreen;
        mHideShippingScreen = hideShippingScreen;
    }

    private ShippingFlowConfig(Parcel in) {
        mHiddenAddressFields = new ArrayList<>();
        in.readStringList(mHiddenAddressFields);
        mOptionalAddressFields = new ArrayList<>();
        in.readStringList(mOptionalAddressFields);
        mPrepopulatedAddress = in.readParcelable(Address.class.getClassLoader());
        mHideAddressScreen = in.readInt() == 1;
        mHideShippingScreen = in.readInt() == 1;
    }


    public List<String> getHiddenAddressFields() {
        return mHiddenAddressFields;
    }

    public List<String> getOptionalAddressFields() {
        return mOptionalAddressFields;
    }

    public Address getPrepopulatedAddress() {
        return mPrepopulatedAddress;
    }

    public boolean isHideAddressScreen() {
        return mHideAddressScreen;
    }

    public boolean isHideShippingScreen() {
        return mHideShippingScreen;
    }

    static final Parcelable.Creator<ShippingFlowConfig> CREATOR
            = new Parcelable.Creator<ShippingFlowConfig>() {

        @Override
        public ShippingFlowConfig createFromParcel(Parcel in) {
            return new ShippingFlowConfig(in);
        }

        @Override
        public ShippingFlowConfig[] newArray(int size) {
            return new ShippingFlowConfig[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeStringList(mHiddenAddressFields);
        parcel.writeStringList(mOptionalAddressFields);
        parcel.writeParcelable(mPrepopulatedAddress, flags);
        parcel.writeInt(mHideAddressScreen ? 1 : 0);
        parcel.writeInt(mHideShippingScreen? 1: 0);
    }
}
