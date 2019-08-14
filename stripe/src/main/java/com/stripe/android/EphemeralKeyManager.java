package com.stripe.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.TimeUnit;

class EphemeralKeyManager<TEphemeralKey extends EphemeralKey> {

    @NonNull private final EphemeralKeyProvider mEphemeralKeyProvider;
    @Nullable private final Calendar mOverrideCalendar;
    @NonNull private final KeyManagerListener<TEphemeralKey> mListener;
    private final long mTimeBufferInSeconds;
    @NonNull private final EphemeralKey.Factory<TEphemeralKey> mFactory;
    @NonNull private final String mApiVersion;

    @Nullable private TEphemeralKey mEphemeralKey;

    EphemeralKeyManager(
            @NonNull EphemeralKeyProvider ephemeralKeyProvider,
            @NonNull KeyManagerListener<TEphemeralKey> keyManagerListener,
            long timeBufferInSeconds,
            @Nullable Calendar overrideCalendar,
            @NonNull OperationIdFactory operationIdFactory,
            @NonNull EphemeralKey.Factory<TEphemeralKey> factory,
            boolean shouldPrefetchEphemeralKey) {
        mFactory = factory;
        mEphemeralKeyProvider = ephemeralKeyProvider;
        mListener = keyManagerListener;
        mTimeBufferInSeconds = timeBufferInSeconds;
        mOverrideCalendar = overrideCalendar;
        mApiVersion = ApiVersion.get().code;

        if (shouldPrefetchEphemeralKey) {
            retrieveEphemeralKey(operationIdFactory.create(), null, null);
        }
    }

    void retrieveEphemeralKey(@NonNull String operationId,
                              @Nullable String actionString,
                              @Nullable Map<String, Object> arguments) {
        if (shouldRefreshKey(
                mEphemeralKey,
                mTimeBufferInSeconds,
                mOverrideCalendar)) {
            mEphemeralKeyProvider.createEphemeralKey(mApiVersion,
                    new ClientKeyUpdateListener(this, operationId, actionString, arguments));
        } else {
            mListener.onKeyUpdate(mEphemeralKey, operationId, actionString, arguments);
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void updateKey(
            @NonNull String operationId,
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
            mEphemeralKey = EphemeralKey.fromJson(new JSONObject(key), mFactory);
            mListener.onKeyUpdate(mEphemeralKey, operationId, actionString, arguments);
        } catch (JSONException e) {
            mListener.onKeyError(operationId,
                    HttpURLConnection.HTTP_INTERNAL_ERROR,
                    "EphemeralKeyUpdateListener.onKeyUpdate was passed " +
                            "a value that could not be JSON parsed: ["
                            + e.getLocalizedMessage() + "]. The raw body from Stripe's response" +
                            " should be passed.");
        } catch (Exception e) {
            mListener.onKeyError(operationId,
                    HttpURLConnection.HTTP_INTERNAL_ERROR,
                    "EphemeralKeyUpdateListener.onKeyUpdate was passed " +
                            "a JSON String that was invalid: ["
                            + e.getLocalizedMessage() + "]. The raw body from Stripe's response" +
                            " should be passed.");
        }
    }

    private void updateKeyError(@NonNull String operationId, int errorCode,
                                @NonNull String errorMessage) {
        mEphemeralKey = null;
        mListener.onKeyError(operationId, errorCode, errorMessage);
    }

    static boolean shouldRefreshKey(
            @Nullable EphemeralKey key,
            long bufferInSeconds,
            @Nullable Calendar proxyCalendar) {
        if (key == null) {
            return true;
        }

        final Calendar now = proxyCalendar == null ? Calendar.getInstance() : proxyCalendar;
        long nowInSeconds = TimeUnit.MILLISECONDS.toSeconds(now.getTimeInMillis());
        long nowPlusBuffer = nowInSeconds + bufferInSeconds;
        return key.getExpires() < nowPlusBuffer;
    }

    interface KeyManagerListener<TEphemeralKey extends EphemeralKey> {
        void onKeyUpdate(@NonNull TEphemeralKey ephemeralKey, @NonNull String operationId,
                         @Nullable String action, @Nullable Map<String, Object> arguments);

        void onKeyError(@NonNull String operationId, int errorCode, @NonNull String errorMessage);
    }

    private static class ClientKeyUpdateListener implements EphemeralKeyUpdateListener {

        @NonNull private final EphemeralKeyManager mEphemeralKeyManager;
        @NonNull private final String mOperationId;
        @Nullable private final String mActionString;
        @Nullable private final Map<String, Object> mArguments;

        ClientKeyUpdateListener(
                @NonNull EphemeralKeyManager ephemeralKeyManager,
                @NonNull String operationId,
                @Nullable String actionString,
                @Nullable Map<String, Object> arguments) {
            mEphemeralKeyManager = ephemeralKeyManager;
            mOperationId = operationId;
            mActionString = actionString;
            mArguments = arguments;
        }

        @Override
        public void onKeyUpdate(@NonNull String rawKey) {
            mEphemeralKeyManager.updateKey(mOperationId, rawKey, mActionString, mArguments);
        }

        @Override
        public void onKeyUpdateFailure(int responseCode, @NonNull String message) {
            mEphemeralKeyManager.updateKeyError(mOperationId, responseCode, message);
        }
    }
}
