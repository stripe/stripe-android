package com.stripe.android;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.stripe.android.utils.ObjectUtils;

import java.util.Objects;

public final class PaymentConfiguration implements Parcelable {

    @Nullable private static PaymentConfiguration mInstance;
    @NonNull private final String mPublishableKey;

    private PaymentConfiguration(@NonNull String publishableKey) {
        mPublishableKey = ApiKeyValidator.get().requireValid(publishableKey);
    }

    private PaymentConfiguration(@NonNull Parcel in) {
        mPublishableKey = Objects.requireNonNull(in.readString());
    }

    public static final Creator<PaymentConfiguration> CREATOR =
            new Creator<PaymentConfiguration>() {
                @Override
                public PaymentConfiguration createFromParcel(@NonNull Parcel in) {
                    return new PaymentConfiguration(in);
                }

                @Override
                public PaymentConfiguration[] newArray(int size) {
                    return new PaymentConfiguration[size];
                }
            };

    @NonNull
    public static PaymentConfiguration getInstance() {
        if (mInstance == null) {
            throw new IllegalStateException(
                    "Attempted to get instance of PaymentConfiguration without initialization.");
        }
        return mInstance;
    }

    /**
     * A publishable key from the Dashboard's
     * <a href="https://dashboard.stripe.com/apikeys">API keys</a> page.
     */
    public static void init(@NonNull String publishableKey) {
        mInstance = new PaymentConfiguration(publishableKey);
    }

    @NonNull
    public String getPublishableKey() {
        return mPublishableKey;
    }

    @VisibleForTesting
    static void clearInstance() {
        mInstance = null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(mPublishableKey);
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hash(mPublishableKey);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return super.equals(obj) ||
                (obj instanceof PaymentConfiguration && typedEquals((PaymentConfiguration) obj));
    }

    private boolean typedEquals(@NonNull PaymentConfiguration obj) {
        return ObjectUtils.equals(mPublishableKey, obj.mPublishableKey);
    }
}
