package com.stripe.android;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.stripe.android.model.Address;
import com.stripe.android.model.ShippingInformation;
import com.stripe.android.view.ShippingInfoWidget;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that tells {@link com.stripe.android.PaymentSession} what functionality it is supporting.
 */
public class PaymentSessionConfig implements Parcelable {

    @NonNull private List<String> mHiddenShippingInfoFields;
    @NonNull private List<String> mOptionalShippingInfoFields;
    @NonNull private ShippingInformation mShippingInformation;
    private boolean mRequireShippingInfo;
    private boolean mRequireShippingMethods;

     public static class Builder {
        private boolean mRequireShippingInfo = true;
        private boolean mRequireShippingMethods = true;
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
         * @param shippingInfo that should be prepopulated into the {@link ShippingInfoWidget}
         * @return
         */
        public Builder setPrepopulatedShippingInfo(ShippingInformation shippingInfo) {
            mShippingInformation = shippingInfo;
            return this;
        }

        /**
         * @param requireShippingInfo whether a {@link ShippingInformation} should be required. If it is
         *                            required, a screen with a {@link ShippingInfoWidget} can be
         *                            shown to collect it.
         */
        public Builder setRequireShippingInfo(boolean requireShippingInfo) {
            mRequireShippingInfo = requireShippingInfo;
            return this;
        }

        /**
         * @param requireShippingMethods whether a {@link com.stripe.android.model.ShippingMethod}
         *                               should be required. If it is required, a screen with a
         *                               {@link com.stripe.android.view.SelectShippingMethodWidget}
         *                               can be shown to collect it.
         */
        public Builder setRequireShippingMethods(boolean requireShippingMethods) {
            mRequireShippingMethods = requireShippingMethods;
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
        mRequireShippingInfo = builder.mRequireShippingInfo;
        mRequireShippingMethods = builder.mRequireShippingMethods;
    }

    private PaymentSessionConfig(Parcel in) {
        mHiddenShippingInfoFields = new ArrayList<>();
        in.readStringList(mHiddenShippingInfoFields);
        mOptionalShippingInfoFields = new ArrayList<>();
        in.readStringList(mOptionalShippingInfoFields);
        mShippingInformation = in.readParcelable(Address.class.getClassLoader());
        mRequireShippingInfo = in.readInt() == 1;
        mRequireShippingMethods = in.readInt() == 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PaymentSessionConfig that = (PaymentSessionConfig) o;

        if (isRequireShippingInfo() != that.isRequireShippingInfo()) return false;
        if (isRequireShippingMethods() != that.isRequireShippingMethods()) return false;
        if (!getHiddenShippingInfoFields().equals(that.getHiddenShippingInfoFields())) return false;
        if (!getOptionalShippingInfoFields().equals(that.getOptionalShippingInfoFields())) return false;
        return getPrepopulatedShippingInfo().equals(that.getPrepopulatedShippingInfo());
    }
    
    @Override
    public int hashCode() {
        int result = getHiddenShippingInfoFields().hashCode();
        result = 31 * result + getOptionalShippingInfoFields().hashCode();
        result = 31 * result + mShippingInformation.hashCode();
        result = 31 * result + (isRequireShippingInfo() ? 1 : 0);
        result = 31 * result + (isRequireShippingMethods() ? 1 : 0);
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
        parcel.writeInt(mRequireShippingInfo ? 1 : 0);
        parcel.writeInt(mRequireShippingMethods ? 1: 0);
    }

    public @NonNull List<String> getHiddenShippingInfoFields() {
        return mHiddenShippingInfoFields;
    }

    public @NonNull List<String> getOptionalShippingInfoFields() {
        return mOptionalShippingInfoFields;
    }

    @NonNull
    public ShippingInformation getPrepopulatedShippingInfo() {
        return mShippingInformation;
    }

    public boolean isRequireShippingInfo() {
        return mRequireShippingInfo;
    }

    public boolean isRequireShippingMethods() {
        return mRequireShippingMethods;
    }

    static final Parcelable.Creator<PaymentSessionConfig> CREATOR
            = new Parcelable.Creator<PaymentSessionConfig>() {

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
