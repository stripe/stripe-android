package com.stripe.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

class EphemeralKeyManager {

    private @Nullable EphemeralKey mEphemeralKey;
    private @NonNull EphemeralKeyProvider mEphemeralKeyProvider;
    private @Nullable Calendar mOverrideCalendar;
    private @NonNull KeyManagerListener mListener;
    private final long mTimeBufferInSeconds;

    EphemeralKeyManager(
            @NonNull EphemeralKeyProvider ephemeralKeyProvider,
            @NonNull KeyManagerListener keyManagerListener,
            long timeBufferInSeconds,
            @Nullable Calendar overrideCalendar) {

        mEphemeralKeyProvider = ephemeralKeyProvider;
        mListener = keyManagerListener;
        mTimeBufferInSeconds = timeBufferInSeconds;
        mOverrideCalendar = overrideCalendar;
        updateKeyIfNecessary();
    }

    @Nullable
    EphemeralKey getEphemeralKey() {
        return mEphemeralKey;
    }

    void updateKeyIfNecessary() {
        if (mEphemeralKey != null &&
                !shouldRefreshKey(
                    mEphemeralKey,
                    mTimeBufferInSeconds,
                    mOverrideCalendar)) {
            return;
        }

        mEphemeralKeyProvider.createEphemeralKey(
                StripeApiHandler.API_VERSION,
                new ClientKeyUpdateListener(this));
    }

    private void updateSessionKey(@NonNull String key) {
        mEphemeralKey = EphemeralKey.fromString(key);
        mListener.onKeyUpdate(mEphemeralKey);
    }

    private void updateSessionKeyError(int errorCode, @Nullable String errorMessage) {
        mEphemeralKey = null;
        mListener.onKeyError(errorCode, errorMessage);
    }

    static boolean shouldRefreshKey(
            @Nullable EphemeralKey key,
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

    interface KeyManagerListener {
        void onKeyUpdate(@Nullable EphemeralKey ephemeralKey);
        void onKeyError(int errorCode, @Nullable String errorMessage);
    }

    private static class ClientKeyUpdateListener implements EphemeralKeyUpdateListener {

        private @NonNull
        WeakReference<EphemeralKeyManager> mEphemeralKeyManagerWeakReference;

        ClientKeyUpdateListener(@NonNull EphemeralKeyManager keyManager) {
            mEphemeralKeyManagerWeakReference = new WeakReference<>(keyManager);
        }

        @Override
        public void onKeyUpdate(@NonNull String rawKey) {
            final EphemeralKeyManager keyManager = mEphemeralKeyManagerWeakReference.get();
            if (keyManager != null) {
                keyManager.updateSessionKey(rawKey);
            }
        }

        @Override
        public void onKeyUpdateFailure(int responseCode, @Nullable String message) {
            final EphemeralKeyManager keyManager = mEphemeralKeyManagerWeakReference.get();
            if (keyManager != null) {
                keyManager.updateSessionKeyError(responseCode, message);
            }
        }
    }
}
