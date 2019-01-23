package com.stripe.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

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
        retrieveEphemeralKey(null, null);
    }

    void retrieveEphemeralKey(@Nullable String actionString, Map<String, Object> arguments) {
        if (shouldRefreshKey(
                mEphemeralKey,
                mTimeBufferInSeconds,
                mOverrideCalendar)) {
            mEphemeralKeyProvider.createEphemeralKey(
                    StripeApiHandler.API_VERSION,
                    new ClientKeyUpdateListener(this, actionString, arguments));
        } else {
            mListener.onKeyUpdate(mEphemeralKey, actionString, arguments);
        }
    }

    @Nullable
    @VisibleForTesting
    TEphemeralKey getEphemeralKey() {
        return mEphemeralKey;
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void updateKey(
            @NonNull String key,
            @Nullable String actionString,
            @Nullable Map<String, Object> arguments) {
        // Key is coming from the user, so even if it's @NonNull annotated we
        // want to double check it
        if (key == null) {
            mListener.onKeyError(HttpURLConnection.HTTP_INTERNAL_ERROR,
                    "EphemeralKeyUpdateListener.onKeyUpdate was called " +
                            "with a null value");
            return;
        }
        try {
            mEphemeralKey = AbstractEphemeralKey.fromString(key, mEphemeralKeyClass);
            mListener.onKeyUpdate(mEphemeralKey, actionString, arguments);
        } catch (JSONException e) {
            mListener.onKeyError(HttpURLConnection.HTTP_INTERNAL_ERROR,
                    "EphemeralKeyUpdateListener.onKeyUpdate was passed " +
                            "a value that could not be JSON parsed: ["
                            + e.getLocalizedMessage() + "]. The raw body from Stripe's response" +
                            " should be passed");
        } catch (Exception e) {
            mListener.onKeyError(HttpURLConnection.HTTP_INTERNAL_ERROR,
                    "EphemeralKeyUpdateListener.onKeyUpdate was passed " +
                            "a value that failed to be parsed: ["
                            + e.getLocalizedMessage() + "]. The raw body from Stripe's response" +
                            " should be passed");
        }
    }

    private void updateKeyError(int errorCode, @Nullable String errorMessage) {
        mEphemeralKey = null;
        mListener.onKeyError(errorCode, errorMessage);
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
        void onKeyUpdate(@NonNull TEphemeralKey ephemeralKey,
                         @Nullable String action,
                         @Nullable Map<String, Object> arguments);

        void onKeyError(int errorCode, @Nullable String errorMessage);
    }

    private static class ClientKeyUpdateListener implements EphemeralKeyUpdateListener {

        private @Nullable String mActionString;
        private @Nullable Map<String, Object> mArguments;
        private @NonNull
        WeakReference<EphemeralKeyManager> mEphemeralKeyManagerWeakReference;

        ClientKeyUpdateListener(
                @NonNull EphemeralKeyManager keyManager,
                @Nullable String actionString,
                @Nullable Map<String, Object> arguments) {
            mEphemeralKeyManagerWeakReference = new WeakReference<>(keyManager);
            mActionString = actionString;
            mArguments = arguments;
        }

        @Override
        public void onKeyUpdate(@NonNull String rawKey) {
            final EphemeralKeyManager keyManager = mEphemeralKeyManagerWeakReference.get();
            if (keyManager != null) {
                keyManager.updateKey(rawKey, mActionString, mArguments);
            }
        }

        @Override
        public void onKeyUpdateFailure(int responseCode, @Nullable String message) {
            final EphemeralKeyManager keyManager = mEphemeralKeyManagerWeakReference.get();
            if (keyManager != null) {
                keyManager.updateKeyError(responseCode, message);
            }
        }
    }
}
