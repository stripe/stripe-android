package com.stripe.android.connect;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

// Implemented in Java to dogfood the API.
public class TestFetchClientSecretTask extends FetchClientSecretTask {
    @Nullable
    public String result = null;

    @Override
    public void fetchClientSecret(@NonNull ResultCallback resultCallback) {
        resultCallback.onResult(result);
    }
}
