package com.stripe.android.testharness;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;

import com.stripe.android.EphemeralKeyProvider;
import com.stripe.android.EphemeralKeyUpdateListener;

/**
 * An {@link EphemeralKeyProvider} to be used in tests that automatically returns test values.
 */
public class TestEphemeralKeyProvider implements EphemeralKeyProvider {

    private static final int INVALID_ERROR_CODE = -1;
    private int mErrorCode = INVALID_ERROR_CODE;
    private @Nullable String mErrorMessage;
    private @Nullable String mRawEphemeralKey;

    public TestEphemeralKeyProvider() {}

    @Override
    public void createEphemeralKey(
            @NonNull @Size(min = 4) String apiVersion,
            @NonNull final EphemeralKeyUpdateListener keyUpdateListener) {
        if (mRawEphemeralKey != null) {
            keyUpdateListener.onKeyUpdate(mRawEphemeralKey);
        } else if (mErrorCode != INVALID_ERROR_CODE) {
            keyUpdateListener.onKeyUpdateFailure(mErrorCode, mErrorMessage);
        }
    }

    public void setNextRawEphemeralKey(@NonNull String rawEphemeralKey) {
        mRawEphemeralKey = rawEphemeralKey;
        mErrorCode = INVALID_ERROR_CODE;
        mErrorMessage = null;
    }

    public void setNextError(int errorCode, @NonNull String errorMessage) {
        mRawEphemeralKey = null;
        mErrorCode = errorCode;
        mErrorMessage = errorMessage;
    }
}
