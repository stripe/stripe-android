package com.stripe.android;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.stripe.android.model.ShippingInformation;
import com.stripe.android.utils.ObjectUtils;
import com.stripe.android.view.ShippingInfoWidget;
import com.stripe.android.view.ShippingInfoWidget.CustomizableShippingField;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Class that tells {@link com.stripe.android.PaymentSession} what functionality it is supporting.
 */
public class PaymentSessionConfig implements Parcelable {

    @NonNull private final List<String> mHiddenShippingInfoFields;
    @NonNull private final List<String> mOptionalShippingInfoFields;
    @Nullable private final ShippingInformation mShippingInformation;
    private final boolean mShippingInfoRequired;
    private final boolean mShippingMethodRequired;
    @LayoutRes private final int mAddPaymentMethodFooter;

     public static class Builder implements ObjectBuilder<PaymentSessionConfig> {
        private boolean mShippingInfoRequired = true;
        private boolean mShippingMethodsRequired = true;
        @Nullable private List<String> mHiddenShippingInfoFields;
        @Nullable private List<String> mOptionalShippingInfoFields;
        @Nullable private ShippingInformation mShippingInformation;
        @LayoutRes private int mAddPaymentMethodFooter;

        /**
         * @param hiddenShippingInfoFields that should be hidden in the {@link ShippingInfoWidget}.
         */
        @NonNull
        public Builder setHiddenShippingInfoFields(
                @CustomizableShippingField @NonNull String ... hiddenShippingInfoFields) {
            mHiddenShippingInfoFields = Arrays.asList(hiddenShippingInfoFields);
            return this;
        }

        /**
         * @param optionalShippingInfoFields that should be optional in the
         * {@link ShippingInfoWidget}
         */
        @NonNull
        public Builder setOptionalShippingInfoFields(
                @CustomizableShippingField @NonNull String ... optionalShippingInfoFields) {
            mOptionalShippingInfoFields = Arrays.asList(optionalShippingInfoFields);
            return this;
        }

        /**
         * @param shippingInfo that should be prepopulated into the {@link ShippingInfoWidget}
         */
        @NonNull
        public Builder setPrepopulatedShippingInfo(@Nullable ShippingInformation shippingInfo) {
            mShippingInformation = shippingInfo;
            return this;
        }

        /**
         * @param shippingInfoRequired whether a {@link ShippingInformation} should be required.
         *                             If it is required, a screen with a {@link ShippingInfoWidget}
         *                             can be shown to collect it.
         */
        @NonNull
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
        @NonNull
        public Builder setShippingMethodsRequired(boolean shippingMethodsRequired) {
            mShippingMethodsRequired = shippingMethodsRequired;
            return this;
        }

        @NonNull
        public Builder setAddPaymentMethodFooter(@LayoutRes int addPaymentMethodFooterLayoutId) {
            mAddPaymentMethodFooter = addPaymentMethodFooterLayoutId;
            return this;
        }

        @NonNull
        public PaymentSessionConfig build() {
            return new PaymentSessionConfig(this);
        }
    }

    private PaymentSessionConfig(@NonNull Builder builder) {
        mHiddenShippingInfoFields = ObjectUtils.getOrDefault(builder.mHiddenShippingInfoFields,
                new ArrayList<String>());
        mOptionalShippingInfoFields = ObjectUtils.getOrDefault(builder.mOptionalShippingInfoFields,
                new ArrayList<String>());
        mShippingInformation = builder.mShippingInformation;
        mShippingInfoRequired = builder.mShippingInfoRequired;
        mShippingMethodRequired = builder.mShippingMethodsRequired;
        mAddPaymentMethodFooter = builder.mAddPaymentMethodFooter;
    }

    private PaymentSessionConfig(@NonNull Parcel in) {
        mHiddenShippingInfoFields = new ArrayList<>();
        in.readList(mHiddenShippingInfoFields, String.class.getClassLoader());
        mOptionalShippingInfoFields = new ArrayList<>();
        in.readList(mOptionalShippingInfoFields, String.class.getClassLoader());
        mShippingInformation = in.readParcelable(ShippingInformation.class.getClassLoader());
        mShippingInfoRequired = in.readInt() == 1;
        mShippingMethodRequired = in.readInt() == 1;
        mAddPaymentMethodFooter = in.readInt();
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o) ||
                (o instanceof PaymentSessionConfig && typedEquals((PaymentSessionConfig) o));
    }

    private boolean typedEquals(@NonNull PaymentSessionConfig obj) {
        return Objects.equals(mHiddenShippingInfoFields, obj.mHiddenShippingInfoFields) &&
                Objects.equals(mOptionalShippingInfoFields, obj.mOptionalShippingInfoFields) &&
                Objects.equals(mShippingInformation, obj.mShippingInformation) &&
                Objects.equals(mShippingInfoRequired, obj.mShippingInfoRequired) &&
                Objects.equals(mShippingMethodRequired, obj.mShippingMethodRequired) &&
                Objects.equals(mAddPaymentMethodFooter, obj.mAddPaymentMethodFooter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mHiddenShippingInfoFields, mOptionalShippingInfoFields,
                mShippingInformation, mShippingInfoRequired, mShippingMethodRequired,
                mAddPaymentMethodFooter);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        parcel.writeList(mHiddenShippingInfoFields);
        parcel.writeList(mOptionalShippingInfoFields);
        parcel.writeParcelable(mShippingInformation, flags);
        parcel.writeInt(mShippingInfoRequired ? 1 : 0);
        parcel.writeInt(mShippingMethodRequired ? 1 : 0);
        parcel.writeInt(mAddPaymentMethodFooter);
    }

    @NonNull
    public List<String> getHiddenShippingInfoFields() {
        return mHiddenShippingInfoFields;
    }

    @NonNull
    public List<String> getOptionalShippingInfoFields() {
        return mOptionalShippingInfoFields;
    }

    @Nullable
    public ShippingInformation getPrepopulatedShippingInfo() {
        return mShippingInformation;
    }

    public boolean isShippingInfoRequired() {
        return mShippingInfoRequired;
    }

    public boolean isShippingMethodRequired() {
        return mShippingMethodRequired;
    }

    @LayoutRes
    public int getAddPaymentMethodFooter() {
         return mAddPaymentMethodFooter;
    }

    public static final Parcelable.Creator<PaymentSessionConfig> CREATOR = new Parcelable
            .Creator<PaymentSessionConfig>() {

        @Override
        public PaymentSessionConfig createFromParcel(@NonNull Parcel in) {
            return new PaymentSessionConfig(in);
        }

        @Override
        public PaymentSessionConfig[] newArray(int size) {
            return new PaymentSessionConfig[size];
        }
    };
}
