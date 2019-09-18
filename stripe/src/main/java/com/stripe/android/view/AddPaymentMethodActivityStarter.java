package com.stripe.android.view;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.stripe.android.ObjectBuilder;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.model.PaymentMethod;
import com.stripe.android.utils.ObjectUtils;

import java.util.Objects;

/**
 * A class to start {@link AddPaymentMethodActivity}. Arguments for the activity can be
 * specified with {@link Args} and constructed with {@link Args.Builder}.
 *
 * <p>The result will be returned with request code {@link #REQUEST_CODE}.</p>
 */
public final class AddPaymentMethodActivityStarter
        extends ActivityStarter<AddPaymentMethodActivity, AddPaymentMethodActivityStarter.Args> {
    public static final int REQUEST_CODE = 6001;

    AddPaymentMethodActivityStarter(@NonNull Activity activity) {
        super(activity, AddPaymentMethodActivity.class, Args.DEFAULT, REQUEST_CODE);
    }

    public static final class Args implements ActivityStarter.Args {
        private static final Args DEFAULT = new Args.Builder().build();

        final boolean shouldAttachToCustomer;
        final boolean shouldRequirePostalCode;
        final boolean isPaymentSessionActive;
        final boolean shouldInitCustomerSessionTokens;
        @NonNull final PaymentMethod.Type paymentMethodType;
        @Nullable final PaymentConfiguration paymentConfiguration;

        @NonNull
        public static AddPaymentMethodActivityStarter.Args create(@NonNull Intent intent) {
            final AddPaymentMethodActivityStarter.Args args =
                    intent.getParcelableExtra(ActivityStarter.Args.EXTRA);
            return Objects.requireNonNull(args);
        }

        private Args(@NonNull AddPaymentMethodActivityStarter.Args.Builder builder) {
            this.shouldAttachToCustomer = builder.mShouldAttachToCustomer;
            this.shouldRequirePostalCode = builder.mShouldRequirePostalCode;
            this.isPaymentSessionActive = builder.mIsPaymentSessionActive;
            this.shouldInitCustomerSessionTokens = builder.mShouldInitCustomerSessionTokens;
            this.paymentMethodType = ObjectUtils.getOrDefault(
                    builder.mPaymentMethodType,
                    PaymentMethod.Type.Card
            );
            this.paymentConfiguration = builder.mPaymentConfiguration;
        }

        private Args(@NonNull Parcel in) {
            this.shouldAttachToCustomer = in.readInt() == 1;
            this.shouldRequirePostalCode = in.readInt() == 1;
            this.isPaymentSessionActive = in.readInt() == 1;
            this.shouldInitCustomerSessionTokens = in.readInt() == 1;
            this.paymentMethodType = PaymentMethod.Type.valueOf(in.readString());
            this.paymentConfiguration =
                    in.readParcelable(PaymentConfiguration.class.getClassLoader());
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            dest.writeInt(shouldAttachToCustomer ? 1 : 0);
            dest.writeInt(shouldRequirePostalCode ? 1 : 0);
            dest.writeInt(isPaymentSessionActive ? 1 : 0);
            dest.writeInt(shouldInitCustomerSessionTokens ? 1 : 0);
            dest.writeString(paymentMethodType.name());
            dest.writeParcelable(paymentConfiguration, 0);
        }

        @Override
        public int hashCode() {
            return Objects.hash(shouldAttachToCustomer, shouldRequirePostalCode,
                    isPaymentSessionActive, shouldInitCustomerSessionTokens, paymentMethodType,
                    paymentConfiguration);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return super.equals(obj) || (obj instanceof Args && typedEquals((Args) obj));
        }

        private boolean typedEquals(@NonNull Args args) {
            return Objects.equals(shouldAttachToCustomer, args.shouldAttachToCustomer) &&
                    Objects.equals(shouldRequirePostalCode, args.shouldRequirePostalCode) &&
                    Objects.equals(isPaymentSessionActive, args.isPaymentSessionActive) &&
                    Objects.equals(shouldInitCustomerSessionTokens,
                            args.shouldInitCustomerSessionTokens) &&
                    Objects.equals(paymentMethodType, args.paymentMethodType) &&
                    Objects.equals(paymentConfiguration, args.paymentConfiguration);

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
            private boolean mShouldAttachToCustomer;
            private boolean mShouldRequirePostalCode;
            private boolean mIsPaymentSessionActive = false;
            private boolean mShouldInitCustomerSessionTokens = true;
            @Nullable private PaymentMethod.Type mPaymentMethodType;
            @Nullable private PaymentConfiguration mPaymentConfiguration;

            /**
             * If true, the created Payment Method will be attached to the current Customer
             * using an already-initialized {@link com.stripe.android.CustomerSession}.
             */
            @NonNull
            public Builder setShouldAttachToCustomer(boolean shouldAttachToCustomer) {
                this.mShouldAttachToCustomer = shouldAttachToCustomer;
                return this;
            }

            /**
             * If true, a postal code field will be shown and validated.
             * Currently, only US ZIP Codes are supported.
             */
            @NonNull
            public Builder setShouldRequirePostalCode(boolean shouldRequirePostalCode) {
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
            Builder setPaymentConfiguration(@Nullable PaymentConfiguration paymentConfiguration) {
                this.mPaymentConfiguration = paymentConfiguration;
                return this;
            }

            @NonNull
            @Override
            public AddPaymentMethodActivityStarter.Args build() {
                return new AddPaymentMethodActivityStarter.Args(this);
            }
        }
    }

    /**
     * The result of a {@link AddPaymentMethodActivity}.
     *
     * <p>Retrieve in <code>#onActivityResult()</code> using {@link #fromIntent(Intent)}.
     */
    public static final class Result implements ActivityStarter.Result {
        @NonNull public final PaymentMethod paymentMethod;

        /**
         * @return the {@link Result} object from the given <code>Intent</code>
         */
        @Nullable
        static Result fromIntent(@NonNull Intent intent) {
            return intent.getParcelableExtra(EXTRA);
        }

        Result(@NonNull PaymentMethod paymentMethod) {
            this.paymentMethod = paymentMethod;
        }

        private Result(@NonNull Parcel parcel) {
            this.paymentMethod = Objects.requireNonNull(
                    parcel.<PaymentMethod>readParcelable(PaymentMethod.class.getClassLoader())
            );
        }

        @NonNull
        @Override
        public Bundle toBundle() {
            final Bundle bundle = new Bundle();
            bundle.putParcelable(EXTRA, this);
            return bundle;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            dest.writeParcelable(paymentMethod, flags);
        }

        @Override
        public int hashCode() {
            return Objects.hash(paymentMethod);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return super.equals(obj) || (obj instanceof Result && typedEquals((Result) obj));
        }

        private boolean typedEquals(@NonNull Result other) {
            return Objects.equals(paymentMethod, other.paymentMethod);
        }

        public static final Creator<Result> CREATOR = new Creator<Result>() {
            @Override
            public Result createFromParcel(Parcel in) {
                return new Result(in);
            }

            @Override
            public Result[] newArray(int size) {
                return new Result[size];
            }
        };
    }
}
