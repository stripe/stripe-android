package com.stripe.android;

import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Size;

public interface EphemeralKeyProvider extends Parcelable {

    void fetchEphemeralKey(
            @NonNull @Size(min = 4) String apiVersion,
            @NonNull final EphemeralKeyUpdateListener keyUpdateListener);

    @NonNull ClassLoader getClassLoader();
}
