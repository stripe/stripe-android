package com.stripe.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.exception.APIConnectionException;
import com.stripe.android.exception.APIException;
import com.stripe.android.exception.AuthenticationException;
import com.stripe.android.exception.CardException;
import com.stripe.android.exception.InvalidRequestException;

import org.json.JSONException;

import java.util.HashMap;
import java.util.Map;

/*
 * Methods for retrieval / update of a Stripe Issuing card
 */
public class IssuingCardPinService
        implements EphemeralKeyManager.KeyManagerListener<IssuingCardEphemeralKey> {

    private static final long KEY_REFRESH_BUFFER_IN_SECONDS = 30L;
    private static final String PIN_RETRIEVE = "PIN_RETRIEVE";
    private static final String PIN_UPDATE = "PIN_UPDATE";
    @NonNull
    private final EphemeralKeyManager<IssuingCardEphemeralKey> mEphemeralKeyManager;

    private IssuingCardPinService(
            @NonNull EphemeralKeyProvider keyProvider
    ) {
        mEphemeralKeyManager = new EphemeralKeyManager<>(
                keyProvider,
                this,
                KEY_REFRESH_BUFFER_IN_SECONDS,
                null,
                IssuingCardEphemeralKey.class);
    }

    /**
     * Create a IssuingCardPinService with the provided {@link EphemeralKeyProvider}.
     *
     * @param keyProvider an {@link EphemeralKeyProvider} used to get
     *                    {@link CustomerEphemeralKey EphemeralKeys} as needed
     */
    public static IssuingCardPinService create(@NonNull EphemeralKeyProvider keyProvider) {
        return new IssuingCardPinService(keyProvider);
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
            @Nullable IssuingCardPinRetrievalListener listener
    ) {

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("cardId", cardId);
        arguments.put("verificationId", verificationId);
        arguments.put("userOneTimeCode", userOneTimeCode);
        arguments.put("listener", listener);

        mEphemeralKeyManager.retrieveEphemeralKey(PIN_RETRIEVE, arguments);
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
            @Nullable IssuingCardPinUpdateListener listener
    ) {

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("cardId", cardId);
        arguments.put("newPin", newPin);
        arguments.put("verificationId", verificationId);
        arguments.put("userOneTimeCode", userOneTimeCode);
        arguments.put("listener", listener);

        mEphemeralKeyManager.retrieveEphemeralKey(PIN_UPDATE, arguments);
    }

    @Override
    public void onKeyUpdate(
            @Nullable IssuingCardEphemeralKey ephemeralKey,
            @Nullable String action,
            @Nullable Map<String, Object> arguments
    ) {

        if (PIN_RETRIEVE.equals(action)) {
            String cardId = (String) arguments.get("cardId");
            String verificationId = (String) arguments.get("verificationId");
            String userOneTimeCode = (String) arguments.get("userOneTimeCode");
            IssuingCardPinRetrievalListener listener = (IssuingCardPinRetrievalListener) arguments.get("listener");

            try {
                String pin = StripeApiHandler.retrieveIssuingCardPin(
                        cardId,
                        verificationId,
                        userOneTimeCode,
                        ephemeralKey.getSecret());

                if (listener != null) {
                    listener.onIssuingCardPinRetrieved(pin);
                }
            } catch (InvalidRequestException e) {
                if (listener != null) {
                    if ("expired".equals(e.getCode())) {
                        listener.onError(
                                CardPinActionError.ONE_TIME_CODE_EXPIRED,
                                "The one-time code has expired",
                                null);
                    }
                    if ("incorrect_code".equals(e.getCode())) {
                        listener.onError(
                                CardPinActionError.ONE_TIME_CODE_INCORRECT,
                                "The one-time code was incorrect",
                                null);
                    }
                    if ("too_many_attempts".equals(e.getCode())) {
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
                }
            } catch (APIConnectionException |
                    APIException |
                    AuthenticationException |
                    JSONException |
                    CardException e) {
                if (listener != null) {
                    listener.onError(
                            CardPinActionError.UNKNOWN_ERROR,
                            "An error occurred retrieving the PIN",
                            e);
                }
            }
        }
        if (PIN_UPDATE.equals(action)) {
            String cardId = (String) arguments.get("cardId");
            String newPin = (String) arguments.get("newPin");
            String verificationId = (String) arguments.get("verificationId");
            String userOneTimeCode = (String) arguments.get("userOneTimeCode");
            IssuingCardPinUpdateListener listener = (IssuingCardPinUpdateListener) arguments.get("listener");

            try {
                StripeApiHandler.updateIssuingCardPin(
                        cardId,
                        newPin,
                        verificationId,
                        userOneTimeCode,
                        ephemeralKey.getSecret());

                if (listener != null) {
                    listener.onIssuingCardPinUpdated();
                }
            } catch (InvalidRequestException e) {
                if (listener != null) {
                    if ("expired".equals(e.getCode())) {
                        listener.onError(
                                CardPinActionError.ONE_TIME_CODE_EXPIRED,
                                "The one-time code has expired",
                                null);
                    }
                    if ("incorrect_code".equals(e.getCode())) {
                        listener.onError(
                                CardPinActionError.ONE_TIME_CODE_INCORRECT,
                                "The one-time code was incorrect",
                                null);
                    }
                    if ("too_many_attempts".equals(e.getCode())) {
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
                }
            } catch (APIConnectionException |
                    APIException |
                    AuthenticationException |
                    CardException e) {
                if (listener != null) {
                    listener.onError(
                            CardPinActionError.UNKNOWN_ERROR,
                            "An error occurred retrieving the PIN",
                            e);
                }
            }
        }
    }

    @Override
    public void onKeyError(int errorCode,
                           @Nullable String errorMessage,
                           @Nullable Map<String, Object> arguments) {
        Object listener = arguments.get("listener");
        if(listener == null){
            return;
        }
        if (listener instanceof IssuingCardPinRetrievalListener) {
            ((IssuingCardPinRetrievalListener) listener).onError(
                    CardPinActionError.EPHEMERAL_KEY_ERROR,
                    errorMessage,
                    null);
        }
        if (listener instanceof IssuingCardPinUpdateListener) {
            ((IssuingCardPinUpdateListener) listener).onError(
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
                CardPinActionError errorCode,
                @Nullable String errorMessage,
                @Nullable Throwable exception);
    }

    public interface IssuingCardPinUpdateListener {
        void onIssuingCardPinUpdated();

        void onError(
                CardPinActionError errorCode,
                @Nullable String errorMessage,
                @Nullable Throwable exception);
    }

}
