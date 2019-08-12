package com.stripe.android.view;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.ObjectBuilder;
import com.stripe.android.model.PaymentMethod;
import com.stripe.android.utils.ObjectUtils;

import java.util.Objects;

public class AddPaymentMethodActivityStarter
        extends ActivityStarter<AddPaymentMethodActivity, AddPaymentMethodActivityStarter.Args> {
    AddPaymentMethodActivityStarter(@NonNull Activity activity) {
        super(activity, AddPaymentMethodActivity.class, Args.DEFAULT);
    }

    public static final class Args implements ActivityStarter.Args {
        private static final Args DEFAULT = new Args.Builder().build();

        final boolean shouldUpdateCustomer;
        final boolean shouldRequirePostalCode;
        final boolean isPaymentSessionActive;
        final boolean shouldInitCustomerSessionTokens;
        @NonNull final PaymentMethod.Type paymentMethodType;

        @NonNull
        public static AddPaymentMethodActivityStarter.Args create(@NonNull Intent intent) {
            final AddPaymentMethodActivityStarter.Args args =
                    intent.getParcelableExtra(ActivityStarter.Args.EXTRA);
            return Objects.requireNonNull(args);
        }

        private Args(@NonNull AddPaymentMethodActivityStarter.Args.Builder builder) {
            this.shouldUpdateCustomer = builder.mShouldUpdateCustomer;
            this.shouldRequirePostalCode = builder.mShouldRequirePostalCode;
            this.isPaymentSessionActive = builder.mIsPaymentSessionActive;
            this.shouldInitCustomerSessionTokens = builder.mShouldInitCustomerSessionTokens;
            this.paymentMethodType = ObjectUtils.getOrDefault(
                    builder.mPaymentMethodType,
                    PaymentMethod.Type.Card
            );
        }

        private Args(@NonNull Parcel in) {
            this.shouldUpdateCustomer = in.readInt() == 1;
            this.shouldRequirePostalCode = in.readInt() == 1;
            this.isPaymentSessionActive = in.readInt() == 1;
            this.shouldInitCustomerSessionTokens = in.readInt() == 1;
            this.paymentMethodType = PaymentMethod.Type.valueOf(in.readString());
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(shouldUpdateCustomer ? 1 : 0);
            dest.writeInt(shouldRequirePostalCode ? 1 : 0);
            dest.writeInt(isPaymentSessionActive ? 1 : 0);
            dest.writeInt(shouldInitCustomerSessionTokens ? 1 : 0);
            dest.writeString(paymentMethodType.name());
        }

        @Override
        public int hashCode() {
            return ObjectUtils.hash(shouldUpdateCustomer, shouldRequirePostalCode,
                    isPaymentSessionActive, shouldInitCustomerSessionTokens, paymentMethodType);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return super.equals(obj) || (obj instanceof Args && typedEquals((Args) obj));
        }

        private boolean typedEquals(@NonNull Args args) {
            return ObjectUtils.equals(shouldUpdateCustomer, args.shouldUpdateCustomer) &&
                    ObjectUtils.equals(shouldRequirePostalCode, args.shouldRequirePostalCode) &&
                    ObjectUtils.equals(isPaymentSessionActive, args.isPaymentSessionActive) &&
                    ObjectUtils.equals(shouldInitCustomerSessionTokens,
                            args.shouldInitCustomerSessionTokens) &&
                    ObjectUtils.equals(paymentMethodType, args.paymentMethodType);
        }

        public static final Parcelable.Creator<AddPaymentMethodActivityStarter.Args> CREATOR =
                new Parcelable.Creator<AddPaymentMethodActivityStarter.Args>() {

                    @Override
                    public AddPaymentMethodActivityStarter.Args createFromParcel(
                            @NonNull Parcel in) {
                        return new AddPaymentMethodActivityStarter.Args(in);
                    }

                    @Override
                    public AddPaymentMethodActivityStarter.Args[] newArray(int size) {
                        return new AddPaymentMethodActivityStarter.Args[size];
                    }
                };

        public static final class Builder
                implements ObjectBuilder<AddPaymentMethodActivityStarter.Args> {
            private boolean mShouldUpdateCustomer;
            private boolean mShouldRequirePostalCode;
            private boolean mIsPaymentSessionActive = false;
            private boolean mShouldInitCustomerSessionTokens = true;
            private PaymentMethod.Type mPaymentMethodType;

            /**
             * If true, update using an already-initialized
             * {@link com.stripe.android.CustomerSession}
             */
            @NonNull
            Builder setShouldUpdateCustomer(boolean shouldUpdateCustomer) {
                this.mShouldUpdateCustomer = shouldUpdateCustomer;
                return this;
            }

            @NonNull
            Builder setShouldRequirePostalCode(boolean shouldRequirePostalCode) {
                this.mShouldRequirePostalCode = shouldRequirePostalCode;
                return this;
            }

            @NonNull
            Builder setIsPaymentSessionActive(boolean isPaymentSessionActive) {
                this.mIsPaymentSessionActive = isPaymentSessionActive;
                return this;
            }

            @NonNull
            Builder setShouldInitCustomerSessionTokens(boolean shouldInitCustomerSessionTokens) {
                this.mShouldInitCustomerSessionTokens = shouldInitCustomerSessionTokens;
                return this;
            }

            @NonNull
            Builder setPaymentMethodType(@NonNull PaymentMethod.Type paymentMethodType) {
                this.mPaymentMethodType = paymentMethodType;
                return this;
            }

            @NonNull
            @Override
            public AddPaymentMethodActivityStarter.Args build() {
                return new AddPaymentMethodActivityStarter.Args(this);
            }
        }
    }
}
