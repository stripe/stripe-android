package com.stripe.android;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.model.Customer;

/**
 * Represents a logged-in session of a single Customer.
 */
public class CustomerSession implements Parcelable {

    private @Nullable EphemeralKey mEphemeralKey;
    private @NonNull EphemeralKeyProvider mKeyProvider;

    private CustomerSession(Parcel in) {
        ClassLoader keyProviderLoader =
                PaymentConfiguration.getInstance().getEphemeralKeyProviderClassLoader();
        if (keyProviderLoader == null) {
            throw new IllegalStateException("Cannot create CustomerSession objects without " +
                    "a KeyProvider with proper ClassLoader");
        }
        mKeyProvider = in.readParcelable(keyProviderLoader);
    }

    public CustomerSession(@NonNull EphemeralKeyProvider keyProvider) {
        mKeyProvider = keyProvider;
        PaymentConfiguration.getInstance().setEphemeralKeyProviderClassLoader(
                keyProvider.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(mKeyProvider, i);
    }

    static final Parcelable.Creator<CustomerSession> CREATOR
            = new Parcelable.Creator<CustomerSession>() {

        @Override
        public CustomerSession createFromParcel(Parcel in) {
            return new CustomerSession(in);
        }

        @Override
        public CustomerSession[] newArray(int size) {
            return new CustomerSession[size];
        }
    };

    public static class KeyUpdateCallback {

        public void onKeyUpdate(String updatedRawKey) {
            EphemeralKey key = EphemeralKey.fromString(updatedRawKey);
        }

        public void onKeyUpdateError(String errorMessage) {

        }
    }

}
