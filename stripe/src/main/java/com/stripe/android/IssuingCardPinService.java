package com.stripe.android;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.stripe.android.exception.APIConnectionException;
import com.stripe.android.exception.APIException;
import com.stripe.android.exception.AuthenticationException;
import com.stripe.android.exception.CardException;
import com.stripe.android.exception.InvalidRequestException;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.json.JSONException;

/**
 * Methods for retrieval / update of a Stripe Issuing card
 */
@SuppressWarnings("WeakerAccess")
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
    private final StripeRepository mStripeRepository;
    @NonNull
    private final OperationIdFactory mOperationIdFactory;
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
     *                    {@link IssuingCardEphemeralKey EphemeralKeys} as needed
     */
    @NonNull
    public static IssuingCardPinService create(
            @NonNull Context context,
            @NonNull EphemeralKeyProvider keyProvider) {
        return new IssuingCardPinService(context, keyProvider, Stripe.getAppInfo());
    }

    private IssuingCardPinService(
            @NonNull Context context,
            @NonNull EphemeralKeyProvider keyProvider,
            @Nullable AppInfo appInfo) {
        this(keyProvider, new StripeApiRepository(context, appInfo), new OperationIdFactory());
    }

    @VisibleForTesting
    IssuingCardPinService(
            @NonNull EphemeralKeyProvider keyProvider,
            @NonNull StripeRepository stripeRepository,
            @NonNull OperationIdFactory operationIdFactory) {
        mOperationIdFactory = operationIdFactory;
        mStripeRepository = stripeRepository;
        mEphemeralKeyManager = new EphemeralKeyManager<>(
                keyProvider,
                this,
                KEY_REFRESH_BUFFER_IN_SECONDS,
                null,
                operationIdFactory,
                new IssuingCardEphemeralKey.Factory(),
                true
        );
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
            @NonNull IssuingCardPinRetrievalListener listener) {
        final Map<String, Object> arguments = new HashMap<>();
        arguments.put(ARGUMENT_CARD_ID, cardId);
        arguments.put(ARGUMENT_VERIFICATION_ID, verificationId);
        arguments.put(ARGUMENT_ONE_TIME_CODE, userOneTimeCode);

        final String operationId = mOperationIdFactory.create();
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
            @NonNull IssuingCardPinUpdateListener listener) {
        final Map<String, Object> arguments = new HashMap<>();
        arguments.put(ARGUMENT_CARD_ID, cardId);
        arguments.put(ARGUMENT_NEW_PIN, newPin);
        arguments.put(ARGUMENT_VERIFICATION_ID, verificationId);
        arguments.put(ARGUMENT_ONE_TIME_CODE, userOneTimeCode);

        final String operationId = mOperationIdFactory.create();
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
                        "Arguments were lost during the ephemeral key call, " +
                                "this is not supposed to happen," +
                                " please contact support@stripe.com for assistance.",
                        null);
                return;
            }

            final String cardId =
                    (String) Objects.requireNonNull(arguments.get(ARGUMENT_CARD_ID));
            final String verificationId =
                    (String) Objects.requireNonNull(arguments.get(ARGUMENT_VERIFICATION_ID));
            final String userOneTimeCode =
                    (String) Objects.requireNonNull(arguments.get(ARGUMENT_ONE_TIME_CODE));
            try {
                final String pin = mStripeRepository.retrieveIssuingCardPin(cardId, verificationId,
                        userOneTimeCode, ephemeralKey.getSecret());
                listener.onIssuingCardPinRetrieved(pin);

            } catch (InvalidRequestException e) {
                if ("expired".equals(e.getErrorCode())) {
                    listener.onError(
                            CardPinActionError.ONE_TIME_CODE_EXPIRED,
                            "The one-time code has expired",
                            null);
                } else if ("incorrect_code".equals(e.getErrorCode())) {
                    listener.onError(
                            CardPinActionError.ONE_TIME_CODE_INCORRECT,
                            "The one-time code was incorrect",
                            null);
                } else if ("too_many_attempts".equals(e.getErrorCode())) {
                    listener.onError(
                            CardPinActionError.ONE_TIME_CODE_TOO_MANY_ATTEMPTS,
                            "The verification challenge was attempted too many times",
                            null);
                } else if ("already_redeemed".equals(e.getErrorCode())) {
                    listener.onError(
                            CardPinActionError.ONE_TIME_CODE_ALREADY_REDEEMED,
                            "The verification challenge was already redeemed",
                            null);
                } else {
                    listener.onError(
                            CardPinActionError.UNKNOWN_ERROR,
                            "The call to retrieve the PIN failed, possibly an error " +
                                    "with the verification. Please check the exception.",
                            e);
                }
            } catch (APIConnectionException |
                    APIException |
                    AuthenticationException |
                    JSONException |
                    CardException e) {
                listener.onError(
                        CardPinActionError.UNKNOWN_ERROR,
                        "An error occurred retrieving the PIN, " +
                                "please check the exception",
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
                        "Arguments were lost during the ephemeral key call, " +
                                "this is not supposed to happen," +
                                " please contact support@stripe.com for assistance.",
                        null);
                return;
            }

            final String cardId =
                    (String) Objects.requireNonNull(arguments.get(ARGUMENT_CARD_ID));
            final String newPin =
                    (String) Objects.requireNonNull(arguments.get(ARGUMENT_NEW_PIN));
            final String verificationId =
                    (String) Objects.requireNonNull(arguments.get(ARGUMENT_VERIFICATION_ID));
            final String userOneTimeCode =
                    (String) Objects.requireNonNull(arguments.get(ARGUMENT_ONE_TIME_CODE));
            try {
                mStripeRepository.updateIssuingCardPin(cardId, newPin, verificationId,
                        userOneTimeCode, ephemeralKey.getSecret());
                listener.onIssuingCardPinUpdated();
            } catch (InvalidRequestException e) {
                if ("expired".equals(e.getErrorCode())) {
                    listener.onError(
                            CardPinActionError.ONE_TIME_CODE_EXPIRED,
                            "The one-time code has expired",
                            null);
                } else if ("incorrect_code".equals(e.getErrorCode())) {
                    listener.onError(
                            CardPinActionError.ONE_TIME_CODE_INCORRECT,
                            "The one-time code was incorrect",
                            null);
                } else if ("too_many_attempts".equals(e.getErrorCode())) {
                    listener.onError(
                            CardPinActionError.ONE_TIME_CODE_TOO_MANY_ATTEMPTS,
                            "The verification challenge was attempted too many times",
                            null);
                } else if ("already_redeemed".equals(e.getErrorCode())) {
                    listener.onError(
                            CardPinActionError.ONE_TIME_CODE_ALREADY_REDEEMED,
                            "The verification challenge was already redeemed",
                            null);
                } else {
                    listener.onError(
                            CardPinActionError.UNKNOWN_ERROR,
                            "The call to update the PIN failed, possibly an error " +
                                    "with the verification. Please check the exception.",
                            e);
                }
            } catch (APIConnectionException |
                    APIException |
                    AuthenticationException |
                    CardException e) {
                listener.onError(
                        CardPinActionError.UNKNOWN_ERROR,
                        "An error occurred retrieving the PIN " +
                                "please check the exception",
                        e);
            }
        }
    }

    @Override
    public void onKeyError(@Nullable String operationId,
                           int errorCode,
                           @NonNull String errorMessage) {

        final IssuingCardPinUpdateListener updateListener = mUpdateListeners.remove(operationId);
        final IssuingCardPinRetrievalListener retrievalListener =
                mRetrievalListeners.remove(operationId);
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
        ONE_TIME_CODE_ALREADY_REDEEMED,
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
