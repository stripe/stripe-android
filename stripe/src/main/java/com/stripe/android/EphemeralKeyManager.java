package com.stripe.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import org.json.JSONException;

import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.TimeUnit;

class EphemeralKeyManager<TEphemeralKey extends AbstractEphemeralKey> {

    @NonNull private final Class<TEphemeralKey> mEphemeralKeyClass;
    private @Nullable TEphemeralKey mEphemeralKey;
    private @NonNull EphemeralKeyProvider mEphemeralKeyProvider;
    private @Nullable Calendar mOverrideCalendar;
    private @NonNull KeyManagerListener mListener;
    private final long mTimeBufferInSeconds;

    EphemeralKeyManager(
            @NonNull EphemeralKeyProvider ephemeralKeyProvider,
            @NonNull KeyManagerListener keyManagerListener,
            long timeBufferInSeconds,
            @Nullable Calendar overrideCalendar,
            @NonNull Class<TEphemeralKey> ephemeralKeyClass) {
        mEphemeralKeyClass = ephemeralKeyClass;
        mEphemeralKeyProvider = ephemeralKeyProvider;
        mListener = keyManagerListener;
        mTimeBufferInSeconds = timeBufferInSeconds;
        mOverrideCalendar = overrideCalendar;
        retrieveEphemeralKey(null, null, null);
    }

    void retrieveEphemeralKey(@Nullable String operationId,
                              @Nullable String actionString,
                              @Nullable Map<String, Object> arguments) {
        if (shouldRefreshKey(
                mEphemeralKey,
                mTimeBufferInSeconds,
                mOverrideCalendar)) {
            mEphemeralKeyProvider.createEphemeralKey(ApiVersion.DEFAULT_API_VERSION,
                    new ClientKeyUpdateListener(this, operationId, actionString, arguments));
        } else {
            mListener.onKeyUpdate(mEphemeralKey, operationId, actionString, arguments);
        }
    }

    @Nullable
    @VisibleForTesting
    TEphemeralKey getEphemeralKey() {
        return mEphemeralKey;
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void updateKey(
            @Nullable String operationId,
            @NonNull String key,
            @Nullable String actionString,
            @Nullable Map<String, Object> arguments) {
        // Key is coming from the user, so even if it's @NonNull annotated we
        // want to double check it
        if (key == null) {
            mListener.onKeyError(operationId,
                    HttpURLConnection.HTTP_INTERNAL_ERROR,
                    "EphemeralKeyUpdateListener.onKeyUpdate was called with a null value");
            return;
        }
        try {
            mEphemeralKey = AbstractEphemeralKey.fromString(key, mEphemeralKeyClass);
            mListener.onKeyUpdate(mEphemeralKey, operationId, actionString, arguments);
        } catch (JSONException e) {
            mListener.onKeyError(operationId,
                    HttpURLConnection.HTTP_INTERNAL_ERROR,
                    "EphemeralKeyUpdateListener.onKeyUpdate was passed " +
                            "a value that could not be JSON parsed: ["
                            + e.getLocalizedMessage() + "]. The raw body from Stripe's response" +
                            " should be passed");
        } catch (Exception e) {
            mListener.onKeyError(operationId,
                    HttpURLConnection.HTTP_INTERNAL_ERROR,
                    "EphemeralKeyUpdateListener.onKeyUpdate was passed " +
                            "a JSON String that was invalid: ["
                            + e.getLocalizedMessage() + "]. The raw body from Stripe's response" +
                            " should be passed");
        }
    }

    private void updateKeyError(@Nullable String operationId, int errorCode,
                                @Nullable String errorMessage) {
        mEphemeralKey = null;
        mListener.onKeyError(operationId, errorCode, errorMessage);
    }

    static boolean shouldRefreshKey(
            @Nullable AbstractEphemeralKey key,
            long bufferInSeconds,
            @Nullable Calendar proxyCalendar) {

        if (key == null) {
            return true;
        }

        Calendar now = proxyCalendar == null ? Calendar.getInstance() : proxyCalendar;
        long nowInSeconds = TimeUnit.MILLISECONDS.toSeconds(now.getTimeInMillis());
        long nowPlusBuffer = nowInSeconds + bufferInSeconds;
        return key.getExpires() < nowPlusBuffer;
    }

    interface KeyManagerListener<TEphemeralKey extends AbstractEphemeralKey> {
        void onKeyUpdate(@NonNull TEphemeralKey ephemeralKey, @Nullable String operationId,
                         @Nullable String action, @Nullable Map<String, Object> arguments);

        void onKeyError(@Nullable String operationId, int errorCode, @Nullable String errorMessage);
    }

    private static class ClientKeyUpdateListener implements EphemeralKeyUpdateListener {

        @Nullable private final String mActionString;
        @Nullable private final String mOperationId;
        @Nullable private final Map<String, Object> mArguments;
        @NonNull private final WeakReference<EphemeralKeyManager> mEphemeralKeyManagerRef;

        ClientKeyUpdateListener(
                @NonNull EphemeralKeyManager keyManager,
                @Nullable String operationId,
                @Nullable String actionString,
                @Nullable Map<String, Object> arguments) {
            mEphemeralKeyManagerRef = new WeakReference<>(keyManager);
            mOperationId = operationId;
            mActionString = actionString;
            mArguments = arguments;
        }

        @Override
        public void onKeyUpdate(@NonNull String rawKey) {
            final EphemeralKeyManager keyManager = mEphemeralKeyManagerRef.get();
            if (keyManager != null) {
                keyManager.updateKey(mOperationId, rawKey, mActionString, mArguments);
            }
        }

        @Override
        public void onKeyUpdateFailure(int responseCode, @Nullable String message) {
            final EphemeralKeyManager keyManager = mEphemeralKeyManagerRef.get();
            if (keyManager != null) {
                keyManager.updateKeyError(mOperationId, responseCode, message);
            }
        }
    }
}
