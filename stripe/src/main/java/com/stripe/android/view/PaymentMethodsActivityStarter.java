package com.stripe.android.view;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.stripe.android.ObjectBuilder;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.model.PaymentMethod;
import com.stripe.android.utils.ObjectUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A class to start {@link PaymentMethodsActivity}. Arguments for the activity can be specified
 * with {@link Args} and constructed with {@link Args.Builder}.
 *
 * <p>The result data is a {@link Result} instance, obtained using
 * {@link Result#fromIntent(Intent)}}. The result will be returned with request code
 * {@link #REQUEST_CODE}.</p>
 */
public final class PaymentMethodsActivityStarter
        extends ActivityStarter<PaymentMethodsActivity, PaymentMethodsActivityStarter.Args> {
    public static final int REQUEST_CODE = 6000;

    public PaymentMethodsActivityStarter(@NonNull Activity activity) {
        super(activity, PaymentMethodsActivity.class, Args.DEFAULT, REQUEST_CODE);
    }

    public PaymentMethodsActivityStarter(@NonNull Fragment fragment) {
        super(fragment, PaymentMethodsActivity.class, Args.DEFAULT, REQUEST_CODE);
    }

    public static final class Args implements ActivityStarter.Args {
        private static final Args DEFAULT = new Builder().build();

        @Nullable final String initialPaymentMethodId;
        public final boolean shouldRequirePostalCode;
        final boolean isPaymentSessionActive;
        @NonNull final Set<PaymentMethod.Type> paymentMethodTypes;
        @Nullable final PaymentConfiguration paymentConfiguration;

        @NonNull
        public static Args create(@NonNull Intent intent) {
            final Args args = intent.getParcelableExtra(ActivityStarter.Args.EXTRA);
            return Objects.requireNonNull(args);
        }

        private Args(@NonNull Builder builder) {
            initialPaymentMethodId = builder.mInitialPaymentMethodId;
            shouldRequirePostalCode = builder.mShouldRequirePostalCode;
            isPaymentSessionActive = builder.mIsPaymentSessionActive;
            paymentMethodTypes = ObjectUtils.getOrEmpty(
                    builder.mPaymentMethodTypes,
                    Collections.singleton(PaymentMethod.Type.Card)
            );
            paymentConfiguration = builder.mPaymentConfiguration;
        }

        private Args(@NonNull Parcel in) {
            initialPaymentMethodId = in.readString();
            shouldRequirePostalCode = in.readInt() == 1;
            isPaymentSessionActive = in.readInt() == 1;

            final int paymentMethodTypesSize = in.readInt();
            paymentMethodTypes = new HashSet<>(paymentMethodTypesSize);
            for (int i = 0; i < paymentMethodTypesSize; i++) {
                paymentMethodTypes.add(PaymentMethod.Type.valueOf(in.readString()));
            }

            paymentConfiguration = in.readParcelable(PaymentConfiguration.class.getClassLoader());
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(initialPaymentMethodId);
            dest.writeInt(shouldRequirePostalCode ? 1 : 0);
            dest.writeInt(isPaymentSessionActive ? 1 : 0);

            dest.writeInt(paymentMethodTypes.size());
            for (PaymentMethod.Type paymentMethodType : paymentMethodTypes) {
                dest.writeString(paymentMethodType.name());
            }

            dest.writeParcelable(paymentConfiguration, 0);
        }

        @Override
        public int hashCode() {
            return Objects.hash(initialPaymentMethodId, shouldRequirePostalCode,
                    isPaymentSessionActive, paymentMethodTypes, paymentConfiguration);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return super.equals(obj) || (obj instanceof Args && typedEquals((Args) obj));
        }

        private boolean typedEquals(@NonNull Args args) {
            return Objects.equals(initialPaymentMethodId, args.initialPaymentMethodId) &&
                    Objects.equals(shouldRequirePostalCode, args.shouldRequirePostalCode) &&
                    Objects.equals(isPaymentSessionActive, args.isPaymentSessionActive) &&
                    Objects.equals(paymentMethodTypes, args.paymentMethodTypes) &&
                    Objects.equals(paymentConfiguration, args.paymentConfiguration);
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
            @Nullable private Set<PaymentMethod.Type> mPaymentMethodTypes;
            @Nullable private PaymentConfiguration mPaymentConfiguration;

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
            public Builder setPaymentConfiguration(
                    @Nullable PaymentConfiguration paymentConfiguration) {
                this.mPaymentConfiguration = paymentConfiguration;
                return this;
            }

            @NonNull
            Builder setPaymentMethodTypes(@NonNull Set<PaymentMethod.Type> paymentMethodTypes) {
                mPaymentMethodTypes = paymentMethodTypes;
                return this;
            }

            @NonNull
            @Override
            public Args build() {
                return new Args(this);
            }
        }
    }

    /**
     * The result of a {@link PaymentMethodsActivity}.
     *
     * <p>Retrieve in <code>#onActivityResult()</code> using {@link #fromIntent(Intent)}.
     */
    public static final class Result implements ActivityStarter.Result {
        @NonNull public final PaymentMethod paymentMethod;
        final boolean useGooglePay;

        /**
         * @return the {@link Result} object from the given <code>Intent</code>
         */
        @Nullable
        public static Result fromIntent(@NonNull Intent intent) {
            return intent.getParcelableExtra(EXTRA);
        }

        public Result(@NonNull PaymentMethod paymentMethod) {
            this.paymentMethod = paymentMethod;
            this.useGooglePay = false;
        }

        private Result(@NonNull Parcel parcel) {
            this.paymentMethod = Objects.requireNonNull(
                    parcel.<PaymentMethod>readParcelable(PaymentMethod.class.getClassLoader())
            );
            this.useGooglePay = parcel.readInt() == 1;
        }

        @NonNull
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
            dest.writeInt(useGooglePay ? 1 : 0);
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
