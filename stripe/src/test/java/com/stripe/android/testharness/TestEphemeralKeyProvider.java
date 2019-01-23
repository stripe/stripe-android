package com.stripe.android.testharness;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;

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
        } else {
            // Useful to test edge cases
            keyUpdateListener.onKeyUpdate(null);
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
