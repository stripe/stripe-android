package com.stripe.android.view;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.stripe.android.ObjectBuilder;
import com.stripe.android.PaymentSessionConfig;
import com.stripe.android.PaymentSessionData;

import java.util.Objects;

public final class PaymentFlowActivityStarter
        extends ActivityStarter<PaymentFlowActivity, PaymentFlowActivityStarter.Args> {
    public PaymentFlowActivityStarter(@NonNull Activity activity) {
        super(activity, PaymentFlowActivity.class, Args.DEFAULT);
    }

    public PaymentFlowActivityStarter(@NonNull Fragment fragment) {
        super(fragment, PaymentFlowActivity.class, Args.DEFAULT);
    }

    public static final class Args implements ActivityStarter.Args {
        private static final Args DEFAULT = new Builder().build();

        @NonNull final PaymentSessionConfig paymentSessionConfig;
        @Nullable final PaymentSessionData paymentSessionData;
        final boolean isPaymentSessionActive;

        @NonNull
        public static PaymentFlowActivityStarter.Args create(@NonNull Intent intent) {
            final PaymentFlowActivityStarter.Args args =
                    intent.getParcelableExtra(ActivityStarter.Args.EXTRA);
            return Objects.requireNonNull(args);
        }

        private Args(@NonNull PaymentFlowActivityStarter.Args.Builder builder) {
            paymentSessionConfig = Objects.requireNonNull(builder.mPaymentSessionConfig);
            paymentSessionData = builder.mPaymentSessionData;
            isPaymentSessionActive = builder.mIsPaymentSessionActive;
        }

        private Args(@NonNull Parcel in) {
            final PaymentSessionConfig paymentSessionConfig =
                    in.readParcelable(PaymentSessionConfig.class.getClassLoader());
            this.paymentSessionConfig = Objects.requireNonNull(paymentSessionConfig);
            this.paymentSessionData = in.readParcelable(PaymentSessionData.class.getClassLoader());
            this.isPaymentSessionActive = in.readInt() == 1;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeParcelable(paymentSessionConfig, 0);
            dest.writeParcelable(paymentSessionData, 0);
            dest.writeInt(isPaymentSessionActive ? 1 : 0);
        }

        public static final Parcelable.Creator<PaymentFlowActivityStarter.Args> CREATOR =
                new Parcelable.Creator<PaymentFlowActivityStarter.Args>() {

            @Override
            public PaymentFlowActivityStarter.Args createFromParcel(@NonNull Parcel in) {
                return new PaymentFlowActivityStarter.Args(in);
            }

            @Override
            public PaymentFlowActivityStarter.Args[] newArray(int size) {
                return new PaymentFlowActivityStarter.Args[size];
            }
        };

        public static final class Builder
                implements ObjectBuilder<Args> {
            @Nullable PaymentSessionConfig mPaymentSessionConfig;
            @Nullable PaymentSessionData mPaymentSessionData;
            private boolean mIsPaymentSessionActive = false;

            @NonNull
            public Builder setPaymentSessionConfig(
                    @Nullable PaymentSessionConfig paymentSessionConfig) {
                this.mPaymentSessionConfig = paymentSessionConfig;
                return this;
            }

            @NonNull
            public Builder setPaymentSessionData(@Nullable PaymentSessionData paymentSessionData) {
                this.mPaymentSessionData = paymentSessionData;
                return this;
            }

            @NonNull
            public Builder setIsPaymentSessionActive(boolean isPaymentSessionActive) {
                this.mIsPaymentSessionActive = isPaymentSessionActive;
                return this;
            }

            @NonNull
            @Override
            public Args build() {
                return new PaymentFlowActivityStarter.Args(this);
            }
        }
    }
}
