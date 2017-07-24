package com.stripe.android;

import android.os.Parcelable;
import android.support.annotation.NonNull;

public interface EphemeralKeyProvider extends Parcelable {

    void fetchEphemeralKey(String apiVersion);

    @NonNull ClassLoader getClassLoader();
}
