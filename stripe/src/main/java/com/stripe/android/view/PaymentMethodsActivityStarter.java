package com.stripe.android.view;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.stripe.android.ObjectBuilder;
import com.stripe.android.model.PaymentMethod;
import com.stripe.android.utils.ObjectUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class PaymentMethodsActivityStarter
        extends ActivityStarter<PaymentMethodsActivity, PaymentMethodsActivityStarter.Args> {
    public PaymentMethodsActivityStarter(@NonNull Activity activity) {
        super(activity, PaymentMethodsActivity.class, Args.DEFAULT);
    }

    public PaymentMethodsActivityStarter(@NonNull Fragment fragment) {
        super(fragment, PaymentMethodsActivity.class, Args.DEFAULT);
    }

    public static final class Args implements ActivityStarter.Args {
        private static final Args DEFAULT = new Builder().build();

        @Nullable final String initialPaymentMethodId;
        public final boolean shouldRequirePostalCode;
        final boolean isPaymentSessionActive;
        @NonNull final Set<PaymentMethod.Type> paymentMethodTypes;

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
        }

        @Override
        public int hashCode() {
            return ObjectUtils.hash(initialPaymentMethodId, shouldRequirePostalCode,
                    isPaymentSessionActive, paymentMethodTypes);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return super.equals(obj) || (obj instanceof Args && typedEquals((Args) obj));
        }

        private boolean typedEquals(@NonNull PaymentMethodsActivityStarter.Args args) {
            return ObjectUtils.equals(initialPaymentMethodId, args.initialPaymentMethodId) &&
                    ObjectUtils.equals(shouldRequirePostalCode, args.shouldRequirePostalCode) &&
                    ObjectUtils.equals(isPaymentSessionActive, args.isPaymentSessionActive) &&
                    ObjectUtils.equals(paymentMethodTypes, args.paymentMethodTypes);
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
            private Set<PaymentMethod.Type> mPaymentMethodTypes;

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
            public Builder setPaymentMethodTypes(
                    @NonNull Set<PaymentMethod.Type> paymentMethodTypes) {
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
}
