package com.stripe.android.connect;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

// Implemented in Java to dogfood the API.
public class TestClientSecretProviderListenerWrapper extends ClientSecretProviderListenerWrapper {
    @Nullable
    public String result = null;

    @Override
    public void provideClientSecret(@NonNull ClientSecretResultListener resultListener) {
        resultListener.onResult(result);
    }
}
