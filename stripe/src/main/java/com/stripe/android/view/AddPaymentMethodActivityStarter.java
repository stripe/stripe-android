package com.stripe.android.view;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.stripe.android.ObjectBuilder;

import java.util.Objects;

public class AddPaymentMethodActivityStarter
        extends ActivityStarter<AddPaymentMethodActivity, AddPaymentMethodActivityStarter.Args> {
    AddPaymentMethodActivityStarter(@NonNull Activity activity) {
        super(activity, AddPaymentMethodActivity.class);
    }

    public static final class Args implements ActivityStarter.Args {
        final boolean shouldUpdateCustomer;
        final boolean shouldRequirePostalCode;
        final boolean isPaymentSessionActive;
        final boolean shouldInitCustomerSessionTokens;

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
        }

        private Args(@NonNull Parcel in) {
            this.shouldUpdateCustomer = in.readInt() == 1;
            this.shouldRequirePostalCode = in.readInt() == 1;
            this.isPaymentSessionActive = in.readInt() == 1;
            this.shouldInitCustomerSessionTokens = in.readInt() == 1;
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
            @Override
            public AddPaymentMethodActivityStarter.Args build() {
                return new AddPaymentMethodActivityStarter.Args(this);
            }
        }
    }
}
