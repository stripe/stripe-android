package com.stripe.android;

import android.content.Context;
import android.content.SharedPreferences;
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

    /**
     * Attempts to load a {@link PaymentConfiguration} instance. First attempt to use the class's
     * singleton instance. If unavailable, attempt to load from {@link Store}.
     *
     * @param context application context
     * @return a {@link PaymentConfiguration} instance, or throw an exception
     */
    @NonNull
    public static PaymentConfiguration getInstance(@NonNull Context context) {
        if (mInstance == null) {
            final PaymentConfiguration loadedInstance = new Store(context).load();
            if (loadedInstance != null) {
                mInstance = loadedInstance;
            } else {
                throw new IllegalStateException("PaymentConfiguration was not initialized");
            }
        }
        return mInstance;
    }

    /**
     * A publishable key from the Dashboard's
     * <a href="https://dashboard.stripe.com/apikeys">API keys</a> page.
     */
    public static void init(@NonNull Context context, @NonNull String publishableKey) {
        mInstance = new PaymentConfiguration(publishableKey);
        new Store(context).save(publishableKey);
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
        return Objects.hash(mPublishableKey);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return super.equals(obj) ||
                (obj instanceof PaymentConfiguration && typedEquals((PaymentConfiguration) obj));
    }

    private boolean typedEquals(@NonNull PaymentConfiguration obj) {
        return Objects.equals(mPublishableKey, obj.mPublishableKey);
    }

    /**
     * Manages saving and loading {@link PaymentConfiguration} data to SharedPreferences.
     */
    private static final class Store {
        @NonNull private final SharedPreferences mPrefs;
        private static final String NAME = PaymentConfiguration.class.getCanonicalName();

        private static final String KEY_PUBLISHABLE_KEY = "key_publishable_key";

        private Store(@NonNull Context context) {
            mPrefs = context.getApplicationContext().getSharedPreferences(NAME, 0);
        }

        private void save(@NonNull String publishableKey) {
            mPrefs.edit()
                    .putString(KEY_PUBLISHABLE_KEY, publishableKey)
                    .apply();
        }

        @Nullable
        private PaymentConfiguration load() {
            final String publishableKey = mPrefs.getString(KEY_PUBLISHABLE_KEY, null);
            if (publishableKey == null) {
                return null;
            }
            return new PaymentConfiguration(publishableKey);
        }
    }
}
