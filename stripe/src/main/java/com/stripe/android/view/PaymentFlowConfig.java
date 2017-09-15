package com.stripe.android.view;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.stripe.android.model.Address;
import com.stripe.android.model.ShippingInformation;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that tells {@link PaymentFlowActivity} what UI to render
 */
public class PaymentFlowConfig implements Parcelable {


    @NonNull private List<String> mHiddenShippingInfoFields;
    @NonNull private List<String> mOptionalShippingInfoFields;
    @NonNull private ShippingInformation mShippingInformation;
    private boolean mHideShippingInfoScreen;
    private boolean mHideShippingMethodsScreen;

     public static class Builder {
        private boolean mHideShippingInfoScreen;
        private boolean mHideShippingMethodsScreen;
        @NonNull private List<String> mHiddenShippingInfoFields;
        @NonNull private List<String> mOptionalShippingInfoFields;
        @NonNull private ShippingInformation mShippingInformation;

        /**
         * @param hiddenShippingInfoFields that should be hidden in the {@link ShippingInfoWidget}.
         */
        public Builder setHiddenShippingInfoFields(List<String> hiddenShippingInfoFields) {
            mHiddenShippingInfoFields = hiddenShippingInfoFields;
            return this;
        }

        /**
         * @param optionalShippingInfoFields that should be optional in the {@link ShippingInfoWidget}
         */
        public Builder setOptionalShippingInfoFields(List<String> optionalShippingInfoFields) {
            mOptionalShippingInfoFields = optionalShippingInfoFields;
            return this;
        }

        /**
         *
         * @param shippingInfo that should be prepopulated into the {@link ShippingInfoWidget}
         * @return
         */
        public Builder setPrepopulatedShippingInfo(ShippingInformation shippingInfo) {
            mShippingInformation = shippingInfo;
            return this;
        }

        /**
         * @param hideShippingInfoScreen whether a screen with a {@link ShippingInfoWidget} to collect
         *                               {@link ShippingInformation} should be shown.
         */
        public Builder setHideShippingInfoScreen(boolean hideShippingInfoScreen) {
            mHideShippingInfoScreen = hideShippingInfoScreen;
            return this;
        }

        /**
         * @param hideShippingMethodsScreen whether a screen with a {@link SelectShippingMethodWidget}
         *                                  to select a {@link com.stripe.android.model.ShippingMethod}
         */
        public Builder setHideShippingMethodsScreen(boolean hideShippingMethodsScreen) {
            mHideShippingMethodsScreen = hideShippingMethodsScreen;
            return this;
        }

        public PaymentFlowConfig build() {
            return new PaymentFlowConfig(this);
        }

    }

    PaymentFlowConfig(Builder builder) {
        mHiddenShippingInfoFields = builder.mHiddenShippingInfoFields;
        mOptionalShippingInfoFields = builder.mOptionalShippingInfoFields;
        mShippingInformation = builder.mShippingInformation;
        mHideShippingInfoScreen = builder.mHideShippingInfoScreen;
        mHideShippingMethodsScreen = builder.mHideShippingMethodsScreen;
    }

    private PaymentFlowConfig(Parcel in) {
        mHiddenShippingInfoFields = new ArrayList<>();
        in.readStringList(mHiddenShippingInfoFields);
        mOptionalShippingInfoFields = new ArrayList<>();
        in.readStringList(mOptionalShippingInfoFields);
        mShippingInformation = in.readParcelable(Address.class.getClassLoader());
        mHideShippingInfoScreen = in.readInt() == 1;
        mHideShippingMethodsScreen = in.readInt() == 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PaymentFlowConfig that = (PaymentFlowConfig) o;

        if (isHideShippingInfoScreen() != that.isHideShippingInfoScreen()) return false;
        if (isHideShippingMethodsScreen() != that.isHideShippingMethodsScreen()) return false;
        if (!getHiddenShippingInfoFields().equals(that.getHiddenShippingInfoFields())) return false;
        if (!getOptionalShippingInfoFields().equals(that.getOptionalShippingInfoFields())) return false;
        return getPrepopulatedShippingInfo().equals(that.getPrepopulatedShippingInfo());
    }
    
    @Override
    public int hashCode() {
        int result = getHiddenShippingInfoFields().hashCode();
        result = 31 * result + getOptionalShippingInfoFields().hashCode();
        result = 31 * result + mShippingInformation.hashCode();
        result = 31 * result + (isHideShippingInfoScreen() ? 1 : 0);
        result = 31 * result + (isHideShippingMethodsScreen() ? 1 : 0);
        return result;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeStringList(mHiddenShippingInfoFields);
        parcel.writeStringList(mOptionalShippingInfoFields);
        parcel.writeParcelable(mShippingInformation, flags);
        parcel.writeInt(mHideShippingInfoScreen ? 1 : 0);
        parcel.writeInt(mHideShippingMethodsScreen ? 1: 0);
    }

    @NonNull List<String> getHiddenShippingInfoFields() {
        return mHiddenShippingInfoFields;
    }

    @NonNull List<String> getOptionalShippingInfoFields() {
        return mOptionalShippingInfoFields;
    }

    @NonNull
    ShippingInformation getPrepopulatedShippingInfo() {
        return mShippingInformation;
    }

    boolean isHideShippingInfoScreen() {
        return mHideShippingInfoScreen;
    }

    boolean isHideShippingMethodsScreen() {
        return mHideShippingMethodsScreen;
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
