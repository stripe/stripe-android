package com.stripe.android;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.stripe.android.model.Address;
import com.stripe.android.model.ShippingInformation;
import com.stripe.android.view.ShippingInfoWidget;
import com.stripe.android.view.ShippingInfoWidget.CustomizableShippingField;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class that tells {@link com.stripe.android.PaymentSession} what functionality it is supporting.
 */
public class PaymentSessionConfig implements Parcelable {

    @NonNull private List<String> mHiddenShippingInfoFields;
    @NonNull private List<String> mOptionalShippingInfoFields;
    @NonNull private ShippingInformation mShippingInformation;
    private boolean mShippingInfoRequired;
    private boolean mShippingMethodRequired;

     public static class Builder {
        private boolean mShippingInfoRequired = true;
        private boolean mShippingMethodsRequired = true;
        @NonNull private List<String> mHiddenShippingInfoFields;
        @NonNull private List<String> mOptionalShippingInfoFields;
        @NonNull private ShippingInformation mShippingInformation;

        /**
         * @param hiddenShippingInfoFields that should be hidden in the {@link ShippingInfoWidget}.
         */
        public Builder setHiddenShippingInfoFields(
                @CustomizableShippingField String ... hiddenShippingInfoFields) {
            mHiddenShippingInfoFields = Arrays.asList(hiddenShippingInfoFields);
            return this;
        }

        /**
         * @param optionalShippingInfoFields that should be optional in the
         * {@link ShippingInfoWidget}
         */
        public Builder setOptionalShippingInfoFields(
                @CustomizableShippingField String ... optionalShippingInfoFields) {
            mOptionalShippingInfoFields = Arrays.asList(optionalShippingInfoFields);
            return this;
        }

        /**
         * @param shippingInfo that should be prepopulated into the {@link ShippingInfoWidget}
         * @return
         */
        public Builder setPrepopulatedShippingInfo(ShippingInformation shippingInfo) {
            mShippingInformation = shippingInfo;
            return this;
        }

        /**
         * @param shippingInfoRequired whether a {@link ShippingInformation} should be required.
         *                             If it is required, a screen with a {@link ShippingInfoWidget}
         *                             can be shown to collect it.
         */
        public Builder setShippingInfoRequired(boolean shippingInfoRequired) {
            mShippingInfoRequired = shippingInfoRequired;
            return this;
        }

        /**
         * @param shippingMethodsRequired whether a {@link com.stripe.android.model.ShippingMethod}
         *                               should be required. If it is required, a screen with a
         *                               {@link com.stripe.android.view.SelectShippingMethodWidget}
         *                               can be shown to collect it.
         */
        public Builder setShippingMethodsRequired(boolean shippingMethodsRequired) {
            mShippingMethodsRequired = shippingMethodsRequired;
            return this;
        }

        public PaymentSessionConfig build() {
            return new PaymentSessionConfig(this);
        }

    }

    PaymentSessionConfig(Builder builder) {
        mHiddenShippingInfoFields = builder.mHiddenShippingInfoFields;
        mOptionalShippingInfoFields = builder.mOptionalShippingInfoFields;
        mShippingInformation = builder.mShippingInformation;
        mShippingInfoRequired = builder.mShippingInfoRequired;
        mShippingMethodRequired = builder.mShippingMethodsRequired;
    }

    private PaymentSessionConfig(Parcel in) {
        mHiddenShippingInfoFields = new ArrayList<>();
        in.readList(mHiddenShippingInfoFields, String.class.getClassLoader());
        mOptionalShippingInfoFields = new ArrayList<>();
        in.readList(mOptionalShippingInfoFields, String.class.getClassLoader());
        mShippingInformation = in.readParcelable(Address.class.getClassLoader());
        mShippingInfoRequired = in.readInt() == 1;
        mShippingMethodRequired = in.readInt() == 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PaymentSessionConfig that = (PaymentSessionConfig) o;

        if (isShippingInfoRequired() != that.isShippingInfoRequired()) return false;
        if (isShippingMethodRequired() != that.isShippingMethodRequired()) return false;
        if (!getHiddenShippingInfoFields().equals(that.getHiddenShippingInfoFields())) return false;
        if (!getOptionalShippingInfoFields().equals(that.getOptionalShippingInfoFields())) {
            return false;
        }
        return getPrepopulatedShippingInfo().equals(that.getPrepopulatedShippingInfo());
    }

    @Override
    public int hashCode() {
        int result = getHiddenShippingInfoFields().hashCode();
        result = 31 * result + getOptionalShippingInfoFields().hashCode();
        result = 31 * result + mShippingInformation.hashCode();
        result = 31 * result + (isShippingInfoRequired() ? 1 : 0);
        result = 31 * result + (isShippingMethodRequired() ? 1 : 0);
        return result;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeList(mHiddenShippingInfoFields);
        parcel.writeList(mOptionalShippingInfoFields);
        parcel.writeParcelable(mShippingInformation, flags);
        parcel.writeInt(mShippingInfoRequired ? 1 : 0);
        parcel.writeInt(mShippingMethodRequired ? 1 : 0);
    }

    public
    @NonNull
    List<String> getHiddenShippingInfoFields() {
        return mHiddenShippingInfoFields;
    }

    public
    @NonNull
    List<String> getOptionalShippingInfoFields() {
        return mOptionalShippingInfoFields;
    }

    @NonNull
    public ShippingInformation getPrepopulatedShippingInfo() {
        return mShippingInformation;
    }

    public boolean isShippingInfoRequired() {
        return mShippingInfoRequired;
    }

    public boolean isShippingMethodRequired() {
        return mShippingMethodRequired;
    }

    public static final Parcelable.Creator<PaymentSessionConfig> CREATOR = new Parcelable
            .Creator<PaymentSessionConfig>() {

        @Override
        public PaymentSessionConfig createFromParcel(Parcel in) {
            return new PaymentSessionConfig(in);
        }

        @Override
        public PaymentSessionConfig[] newArray(int size) {
            return new PaymentSessionConfig[size];
        }
    };
}
