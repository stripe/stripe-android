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

    private static IssuingCardPinService mInstance;
    private @NonNull
    EphemeralKeyManager<IssuingCardEphemeralKey> mEphemeralKeyManager;
    private IssuingCardPinRetrievalListener mCardPinRetrievalListener;
    private IssuingCardPinUpdateListener mCardPinUpdateListener;

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
    public static IssuingCardPinService init(@NonNull EphemeralKeyProvider keyProvider) {
        mInstance = new IssuingCardPinService(keyProvider);
        return mInstance;
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
        mCardPinUpdateListener = null;
        mCardPinRetrievalListener = listener;

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("cardId", cardId);
        arguments.put("verificationId", verificationId);
        arguments.put("userOneTimeCode", userOneTimeCode);

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
        mCardPinRetrievalListener = null;
        mCardPinUpdateListener = listener;

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("cardId", cardId);
        arguments.put("newPin", newPin);
        arguments.put("verificationId", verificationId);
        arguments.put("userOneTimeCode", userOneTimeCode);

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
            try {
                String pin = StripeApiHandler.retrieveIssuingCardPin(
                        cardId,
                        verificationId,
                        userOneTimeCode,
                        ephemeralKey.getSecret());

                if (mCardPinRetrievalListener != null) {
                    mCardPinRetrievalListener.onIssuingCardPinRetrieved(pin);
                }
            } catch (InvalidRequestException e) {
                if (mCardPinRetrievalListener != null) {
                    if ("expired".equals(e.getCode())) {
                        mCardPinRetrievalListener.onError(
                                CardPinActionError.ONE_TIME_CODE_EXPIRED,
                                "The one-time code has expired",
                                null);
                    }
                    if ("incorrect_code".equals(e.getCode())) {
                        mCardPinRetrievalListener.onError(
                                CardPinActionError.ONE_TIME_CODE_INCORRECT,
                                "The one-time code was incorrect",
                                null);
                    }
                    if ("too_many_attempts".equals(e.getCode())) {
                        mCardPinRetrievalListener.onError(
                                CardPinActionError.ONE_TIME_CODE_TOO_MANY_ATTEMPTS,
                                "The verification challenge was attempted too many times",
                                null);
                    } else {
                        mCardPinRetrievalListener.onError(
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
                if (mCardPinRetrievalListener != null) {
                    mCardPinRetrievalListener.onError(
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
            try {
                StripeApiHandler.updateIssuingCardPin(
                        cardId,
                        newPin,
                        verificationId,
                        userOneTimeCode,
                        ephemeralKey.getSecret());

                if (mCardPinUpdateListener != null) {
                    mCardPinUpdateListener.onIssuingCardPinUpdated();
                }
            } catch (InvalidRequestException e) {
                if (mCardPinUpdateListener != null) {
                    if ("expired".equals(e.getCode())) {
                        mCardPinUpdateListener.onError(
                                CardPinActionError.ONE_TIME_CODE_EXPIRED,
                                "The one-time code has expired",
                                null);
                    }
                    if ("incorrect_code".equals(e.getCode())) {
                        mCardPinUpdateListener.onError(
                                CardPinActionError.ONE_TIME_CODE_INCORRECT,
                                "The one-time code was incorrect",
                                null);
                    }
                    if ("too_many_attempts".equals(e.getCode())) {
                        mCardPinUpdateListener.onError(
                                CardPinActionError.ONE_TIME_CODE_TOO_MANY_ATTEMPTS,
                                "The verification challenge was attempted too many times",
                                null);
                    } else {
                        mCardPinUpdateListener.onError(
                                CardPinActionError.UNKNOWN_ERROR,
                                "An error occurred retrieving the PIN",
                                e);
                    }
                }
            } catch (APIConnectionException |
                    APIException |
                    AuthenticationException |
                    CardException e) {
                if (mCardPinUpdateListener != null) {
                    mCardPinUpdateListener.onError(
                            CardPinActionError.UNKNOWN_ERROR,
                            "An error occurred retrieving the PIN",
                            e);
                }
            }
        }
    }

    @Override
    public void onKeyError(int errorCode, @Nullable String errorMessage) {
        if (mCardPinRetrievalListener != null) {
            mCardPinRetrievalListener.onError(
                    CardPinActionError.EPHEMERAL_KEY_ERROR,
                    errorMessage,
                    null);
        }
        if (mCardPinUpdateListener != null) {
            mCardPinUpdateListener.onError(
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
