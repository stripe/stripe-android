package com.stripe.android.view;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.stripe.android.ObjectBuilder;

import java.util.Objects;

public final class PaymentMethodsActivityStarter
        extends ActivityStarter<PaymentMethodsActivity, PaymentMethodsActivityStarter.Args> {
    public PaymentMethodsActivityStarter(@NonNull Activity activity) {
        super(activity, PaymentMethodsActivity.class);
    }

    public PaymentMethodsActivityStarter(@NonNull Fragment fragment) {
        super(fragment, PaymentMethodsActivity.class);
    }

    public static final class Args implements ActivityStarter.Args {
        @Nullable final String initialPaymentMethodId;
        public final boolean shouldRequirePostalCode;
        final boolean isPaymentSessionActive;

        @NonNull
        public static Args create(@NonNull Intent intent) {
            final Args args = intent.getParcelableExtra(ActivityStarter.Args.EXTRA);
            return Objects.requireNonNull(args);
        }

        private Args(@NonNull Builder builder) {
            initialPaymentMethodId = builder.mInitialPaymentMethodId;
            shouldRequirePostalCode = builder.mShouldRequirePostalCode;
            isPaymentSessionActive = builder.mIsPaymentSessionActive;
        }

        private Args(@NonNull Parcel in) {
            initialPaymentMethodId = in.readString();
            shouldRequirePostalCode = in.readInt() == 1;
            isPaymentSessionActive = in.readInt() == 1;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(initialPaymentMethodId);
            dest.writeInt(shouldRequirePostalCode ? 1 : 0);
            dest.writeInt(shouldRequirePostalCode ? 1 : 0);
        }

        public static final Parcelable.Creator<Args> CREATOR = new Parcelable.Creator<Args>() {

            @Override
            public Args createFromParcel(@NonNull Parcel in) {
                return new Args(in);
            }

            @Override
            public Args[] newArray(int size) {
                return new Args[size];
            }
        };

        public static final class Builder implements ObjectBuilder<Args> {
            @Nullable private String mInitialPaymentMethodId = null;
            private boolean mShouldRequirePostalCode = false;
            private boolean mIsPaymentSessionActive = false;

            @NonNull
            public Builder setInitialPaymentMethodId(@Nullable String initialPaymentMethodId) {
                this.mInitialPaymentMethodId = initialPaymentMethodId;
                return this;
            }

            @NonNull
            public Builder setShouldRequirePostalCode(boolean shouldRequirePostalCode) {
                this.mShouldRequirePostalCode = shouldRequirePostalCode;
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
                return new Args(this);
            }
        }
    }
}
