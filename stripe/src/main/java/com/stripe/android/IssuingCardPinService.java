package com.stripe.android;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import com.stripe.android.exception.APIConnectionException;
import com.stripe.android.exception.APIException;
import com.stripe.android.exception.AuthenticationException;
import com.stripe.android.exception.CardException;
import com.stripe.android.exception.InvalidRequestException;

import org.json.JSONException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/*
 * Methods for retrieval / update of a Stripe Issuing card
 */
public class IssuingCardPinService
        implements EphemeralKeyManager.KeyManagerListener<IssuingCardEphemeralKey> {

    private static final String TAG = IssuingCardPinService.class.getName();
    private static final long KEY_REFRESH_BUFFER_IN_SECONDS = 30L;
    private static final String PIN_RETRIEVE = "PIN_RETRIEVE";
    private static final String PIN_UPDATE = "PIN_UPDATE";
    private static final String ARGUMENT_CARD_ID = "cardId";
    private static final String ARGUMENT_VERIFICATION_ID = "verificationId";
    private static final String ARGUMENT_ONE_TIME_CODE = "userOneTimeCode";
    private static final String ARGUMENT_NEW_PIN = "newPin";

    @NonNull
    private final EphemeralKeyManager<IssuingCardEphemeralKey> mEphemeralKeyManager;
    @NonNull
    private final StripeApiHandler mApiHandler;
    @NonNull
    private final Map<String, IssuingCardPinRetrievalListener> mRetrievalListeners =
            new HashMap<>();
    @NonNull
    private final Map<String, IssuingCardPinUpdateListener> mUpdateListeners =
            new HashMap<>();

    /**
     * Create a IssuingCardPinService with the provided {@link EphemeralKeyProvider}.
     *
     * @param keyProvider an {@link EphemeralKeyProvider} used to get
     *                    {@link CustomerEphemeralKey EphemeralKeys} as needed
     */
    @NonNull
    public static IssuingCardPinService create(
            @NonNull Context context,
            @NonNull EphemeralKeyProvider keyProvider) {
        return new IssuingCardPinService(keyProvider, new StripeApiHandler(context));
    }

    @VisibleForTesting
    IssuingCardPinService(
            @NonNull EphemeralKeyProvider keyProvider,
            @NonNull StripeApiHandler apiHandler
    ) {
        mEphemeralKeyManager = new EphemeralKeyManager<>(
                keyProvider,
                this,
                KEY_REFRESH_BUFFER_IN_SECONDS,
                null,
                IssuingCardEphemeralKey.class);
        mApiHandler = apiHandler;
    }

    /**
     * Retrieves a PIN for a given card
     *
     * @param cardId          the ID of the card (looks like ic_abcdef1234)
     * @param verificationId  the ID of the verification that was sent to the cardholder
     *                        (typically server-side, through /v1/issuing/verifications)
     * @param userOneTimeCode the one-time code that was sent to the cardholder through sms or email
     * @param listener        a listener for either the PIN, or any error that can occur
     */
    public void retrievePin(
            @NonNull String cardId,
            @NonNull String verificationId,
            @NonNull String userOneTimeCode,
            @NonNull IssuingCardPinRetrievalListener listener
    ) {

        Map<String, Object> arguments = new HashMap<>();
        arguments.put(ARGUMENT_CARD_ID, cardId);
        arguments.put(ARGUMENT_VERIFICATION_ID, verificationId);
        arguments.put(ARGUMENT_ONE_TIME_CODE, userOneTimeCode);

        final String operationId = UUID.randomUUID().toString();
        mRetrievalListeners.put(operationId, listener);
        mEphemeralKeyManager.retrieveEphemeralKey(operationId, PIN_RETRIEVE, arguments);
    }

    /**
     * Retrieves a PIN for a given card
     *
     * @param cardId          the ID of the card (looks like ic_abcdef1234)
     * @param newPin          the new desired PIN
     * @param verificationId  the ID of the verification that was sent to the cardholder
     *                        (typically server-side, through /v1/issuing/verifications)
     * @param userOneTimeCode the one-time code that was sent to the cardholder through sms or email
     * @param listener        a listener for either the PIN, or any error that can occur
     */
    public void updatePin(
            @NonNull String cardId,
            @NonNull String newPin,
            @NonNull String verificationId,
            @NonNull String userOneTimeCode,
            @NonNull IssuingCardPinUpdateListener listener
    ) {

        Map<String, Object> arguments = new HashMap<>();
        arguments.put(ARGUMENT_CARD_ID, cardId);
        arguments.put(ARGUMENT_NEW_PIN, newPin);
        arguments.put(ARGUMENT_VERIFICATION_ID, verificationId);
        arguments.put(ARGUMENT_ONE_TIME_CODE, userOneTimeCode);

        final String operationId = UUID.randomUUID().toString();
        mUpdateListeners.put(operationId, listener);
        mEphemeralKeyManager.retrieveEphemeralKey(operationId, PIN_UPDATE, arguments);
    }

    @Override
    public void onKeyUpdate(@NonNull IssuingCardEphemeralKey ephemeralKey,
                            @Nullable String operationId,
                            @Nullable String action,
                            @Nullable Map<String, Object> arguments) {

        if (PIN_RETRIEVE.equals(action)) {
            IssuingCardPinRetrievalListener listener = mRetrievalListeners.remove(operationId);
            if (listener == null) {
                Log.e(TAG, IssuingCardPinService.class.getName() +
                        " was called without a listener");
                return;
            }
            if (arguments == null) {
                listener.onError(
                        CardPinActionError.UNKNOWN_ERROR,
                        "An error occurred retrieving the PIN",
                        null);
                return;
            }
            String cardId = (String) arguments.get(ARGUMENT_CARD_ID);
            String verificationId = (String) arguments.get(ARGUMENT_VERIFICATION_ID);
            String userOneTimeCode = (String) arguments.get(ARGUMENT_ONE_TIME_CODE);

            try {

                String pin = mApiHandler.retrieveIssuingCardPin(
                        cardId,
                        verificationId,
                        userOneTimeCode,
                        ephemeralKey.getSecret());
                listener.onIssuingCardPinRetrieved(pin);

            } catch (InvalidRequestException e) {
                if ("expired".equals(e.getErrorCode())) {
                    listener.onError(
                            CardPinActionError.ONE_TIME_CODE_EXPIRED,
                            "The one-time code has expired",
                            null);
                }
                if ("incorrect_code".equals(e.getErrorCode())) {
                    listener.onError(
                            CardPinActionError.ONE_TIME_CODE_INCORRECT,
                            "The one-time code was incorrect",
                            null);
                }
                if ("too_many_attempts".equals(e.getErrorCode())) {
                    listener.onError(
                            CardPinActionError.ONE_TIME_CODE_TOO_MANY_ATTEMPTS,
                            "The verification challenge was attempted too many times",
                            null);
                } else {
                    listener.onError(
                            CardPinActionError.UNKNOWN_ERROR,
                            "An error occurred retrieving the PIN",
                            e);
                }
            } catch (APIConnectionException |
                    APIException |
                    AuthenticationException |
                    JSONException |
                    CardException e) {
                listener.onError(
                        CardPinActionError.UNKNOWN_ERROR,
                        "An error occurred retrieving the PIN",
                        e);
            }
        }
        if (PIN_UPDATE.equals(action)) {

            IssuingCardPinUpdateListener listener = mUpdateListeners.remove(operationId);
            if (listener == null) {
                Log.e(TAG, IssuingCardPinService.class.getName() +
                        " was called without a listener");
                return;
            }
            if (arguments == null) {
                listener.onError(
                        CardPinActionError.UNKNOWN_ERROR,
                        "An error occurred retrieving the PIN",
                        null);
                return;
            }
            String cardId = (String) arguments.get(ARGUMENT_CARD_ID);
            String newPin = (String) arguments.get(ARGUMENT_NEW_PIN);
            String verificationId = (String) arguments.get(ARGUMENT_VERIFICATION_ID);
            String userOneTimeCode = (String) arguments.get(ARGUMENT_ONE_TIME_CODE);

            try {

                mApiHandler.updateIssuingCardPin(
                        cardId,
                        newPin,
                        verificationId,
                        userOneTimeCode,
                        ephemeralKey.getSecret());
                listener.onIssuingCardPinUpdated();

            } catch (InvalidRequestException e) {
                if ("expired".equals(e.getErrorCode())) {
                    listener.onError(
                            CardPinActionError.ONE_TIME_CODE_EXPIRED,
                            "The one-time code has expired",
                            null);
                }
                if ("incorrect_code".equals(e.getErrorCode())) {
                    listener.onError(
                            CardPinActionError.ONE_TIME_CODE_INCORRECT,
                            "The one-time code was incorrect",
                            null);
                }
                if ("too_many_attempts".equals(e.getErrorCode())) {
                    listener.onError(
                            CardPinActionError.ONE_TIME_CODE_TOO_MANY_ATTEMPTS,
                            "The verification challenge was attempted too many times",
                            null);
                } else {
                    listener.onError(
                            CardPinActionError.UNKNOWN_ERROR,
                            "An error occurred retrieving the PIN",
                            e);
                }
            } catch (APIConnectionException |
                    APIException |
                    AuthenticationException |
                    CardException e) {
                listener.onError(
                        CardPinActionError.UNKNOWN_ERROR,
                        "An error occurred retrieving the PIN",
                        e);
            }
        }
    }

    @Override
    public void onKeyError(@Nullable String operationId,
                           int errorCode,
                           @Nullable String errorMessage) {

        IssuingCardPinUpdateListener updateListener = mUpdateListeners.remove(operationId);
        IssuingCardPinRetrievalListener retrievalListener = mRetrievalListeners.remove(operationId);
        if (retrievalListener != null) {
            retrievalListener.onError(
                    CardPinActionError.EPHEMERAL_KEY_ERROR,
                    errorMessage,
                    null);
        } else if (updateListener != null) {
            updateListener.onError(
                    CardPinActionError.EPHEMERAL_KEY_ERROR,
                    errorMessage,
                    null);
        }
    }

    public enum CardPinActionError {
        UNKNOWN_ERROR,
        EPHEMERAL_KEY_ERROR,
        ONE_TIME_CODE_INCORRECT,
        ONE_TIME_CODE_EXPIRED,
        ONE_TIME_CODE_TOO_MANY_ATTEMPTS,
    }

    public interface IssuingCardPinRetrievalListener {
        void onIssuingCardPinRetrieved(@NonNull String pin);

        void onError(
                @NonNull CardPinActionError errorCode,
                @Nullable String errorMessage,
                @Nullable Throwable exception);
    }

    public interface IssuingCardPinUpdateListener {
        void onIssuingCardPinUpdated();

        void onError(
                @NonNull CardPinActionError errorCode,
                @Nullable String errorMessage,
                @Nullable Throwable exception);
    }

}
