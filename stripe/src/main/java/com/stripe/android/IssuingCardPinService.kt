package com.stripe.android

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.stripe.android.EphemeralKeyManager.KeyManagerListener
import com.stripe.android.Stripe.Companion.appInfo
import com.stripe.android.exception.APIConnectionException
import com.stripe.android.exception.APIException
import com.stripe.android.exception.AuthenticationException
import com.stripe.android.exception.CardException
import com.stripe.android.exception.InvalidRequestException
import org.json.JSONException

/**
 * Methods for retrieval / update of a Stripe Issuing card
 */
class IssuingCardPinService @VisibleForTesting internal constructor(
    keyProvider: EphemeralKeyProvider,
    private val stripeRepository: StripeRepository,
    private val operationIdFactory: OperationIdFactory = StripeOperationIdFactory()
) {
    private val retrievalListeners: MutableMap<String?, IssuingCardPinRetrievalListener> =
        mutableMapOf()
    private val updateListeners: MutableMap<String?, IssuingCardPinUpdateListener> =
        mutableMapOf()

    private val ephemeralKeyManager = EphemeralKeyManager(
        keyProvider,
        object : KeyManagerListener {
            override fun onKeyUpdate(
                ephemeralKey: EphemeralKey,
                operation: EphemeralOperation
            ) {
                when (operation) {
                    is EphemeralOperation.Issuing.RetrievePin -> {
                        retrievalListeners.remove(operation.id)?.let { listener ->
                            fireRetrievePinRequest(
                                ephemeralKey,
                                operation,
                                listener
                            )
                        } ?: logMissingListener()
                    }
                    is EphemeralOperation.Issuing.UpdatePin ->
                        updateListeners.remove(operation.id)?.let { listener ->
                            fireUpdatePinRequest(
                                ephemeralKey,
                                operation,
                                listener
                            )
                        } ?: logMissingListener()
                }
            }

            override fun onKeyError(
                operationId: String,
                errorCode: Int,
                errorMessage: String
            ) {
                val updateListener = updateListeners.remove(operationId)
                val retrievalListener = retrievalListeners.remove(operationId)
                retrievalListener?.onError(
                    CardPinActionError.EPHEMERAL_KEY_ERROR,
                    errorMessage,
                    null
                ) ?: updateListener?.onError(
                    CardPinActionError.EPHEMERAL_KEY_ERROR,
                    errorMessage,
                    null
                )
            }
        },
        KEY_REFRESH_BUFFER_IN_SECONDS,
        operationIdFactory,
        true
    )

    /**
     * Retrieves a PIN for a given card
     *
     * @param cardId the ID of the card (looks like ic_abcdef1234)
     * @param verificationId the ID of the verification that was sent to the cardholder
     * (typically server-side, through /v1/issuing/verifications)
     * @param userOneTimeCode the one-time code that was sent to the cardholder through sms or email
     * @param listener a listener for either the PIN, or any error that can occur
     */
    fun retrievePin(
        cardId: String,
        verificationId: String,
        userOneTimeCode: String,
        listener: IssuingCardPinRetrievalListener
    ) {
        val operationId = operationIdFactory.create()
        retrievalListeners[operationId] = listener
        ephemeralKeyManager.retrieveEphemeralKey(
            EphemeralOperation.Issuing.RetrievePin(
                cardId = cardId,
                verificationId = verificationId,
                userOneTimeCode = userOneTimeCode,
                id = operationId
            )
        )
    }

    /**
     * Retrieves a PIN for a given card
     *
     * @param cardId the ID of the card (looks like ic_abcdef1234)
     * @param newPin the new desired PIN
     * @param verificationId the ID of the verification that was sent to the cardholder
     * (typically server-side, through /v1/issuing/verifications)
     * @param userOneTimeCode the one-time code that was sent to the cardholder through sms or email
     * @param listener a listener for either the PIN, or any error that can occur
     */
    fun updatePin(
        cardId: String,
        newPin: String,
        verificationId: String,
        userOneTimeCode: String,
        listener: IssuingCardPinUpdateListener
    ) {
        val operationId = operationIdFactory.create()
        updateListeners[operationId] = listener
        ephemeralKeyManager.retrieveEphemeralKey(
            EphemeralOperation.Issuing.UpdatePin(
                cardId = cardId,
                newPin = newPin,
                verificationId = verificationId,
                userOneTimeCode = userOneTimeCode,
                id = operationId
            )
        )
    }

    private fun fireRetrievePinRequest(
        ephemeralKey: EphemeralKey,
        operation: EphemeralOperation.Issuing.RetrievePin,
        listener: IssuingCardPinRetrievalListener?
    ) {
        if (listener == null) {
            logMissingListener()
            return
        }

        try {
            val pin = stripeRepository.retrieveIssuingCardPin(
                operation.cardId,
                operation.verificationId,
                operation.userOneTimeCode,
                ephemeralKey.secret
            )
            listener.onIssuingCardPinRetrieved(pin)
        } catch (e: InvalidRequestException) {
            when (e.errorCode) {
                "expired" -> {
                    listener.onError(
                        CardPinActionError.ONE_TIME_CODE_EXPIRED,
                        "The one-time code has expired",
                        null)
                }
                "incorrect_code" -> {
                    listener.onError(
                        CardPinActionError.ONE_TIME_CODE_INCORRECT,
                        "The one-time code was incorrect",
                        null)
                }
                "too_many_attempts" -> {
                    listener.onError(
                        CardPinActionError.ONE_TIME_CODE_TOO_MANY_ATTEMPTS,
                        "The verification challenge was attempted too many times",
                        null)
                }
                "already_redeemed" -> {
                    listener.onError(
                        CardPinActionError.ONE_TIME_CODE_ALREADY_REDEEMED,
                        "The verification challenge was already redeemed",
                        null)
                }
                else -> {
                    listener.onError(
                        CardPinActionError.UNKNOWN_ERROR,
                        "The call to retrieve the PIN failed, possibly an error " +
                            "with the verification. Please check the exception.",
                        e)
                }
            }
        } catch (e: APIConnectionException) {
            listener.onError(
                CardPinActionError.UNKNOWN_ERROR,
                "An error occurred retrieving the PIN, " +
                    "please check the exception",
                e)
        } catch (e: APIException) {
            listener.onError(
                CardPinActionError.UNKNOWN_ERROR,
                "An error occurred retrieving the PIN, " +
                    "please check the exception",
                e)
        } catch (e: AuthenticationException) {
            listener.onError(
                CardPinActionError.UNKNOWN_ERROR,
                "An error occurred retrieving the PIN, " +
                    "please check the exception",
                e)
        } catch (e: JSONException) {
            listener.onError(
                CardPinActionError.UNKNOWN_ERROR,
                "An error occurred retrieving the PIN, " +
                    "please check the exception",
                e)
        } catch (e: CardException) {
            listener.onError(
                CardPinActionError.UNKNOWN_ERROR,
                "An error occurred retrieving the PIN, " +
                    "please check the exception",
                e)
        }
    }

    private fun fireUpdatePinRequest(
        ephemeralKey: EphemeralKey,
        operation: EphemeralOperation.Issuing.UpdatePin,
        listener: IssuingCardPinUpdateListener?
    ) {
        if (listener == null) {
            logMissingListener()
            return
        }

        try {
            stripeRepository.updateIssuingCardPin(
                operation.cardId,
                operation.newPin,
                operation.verificationId,
                operation.userOneTimeCode,
                ephemeralKey.secret
            )
            listener.onIssuingCardPinUpdated()
        } catch (e: InvalidRequestException) {
            when (e.errorCode) {
                "expired" -> {
                    listener.onError(
                        CardPinActionError.ONE_TIME_CODE_EXPIRED,
                        "The one-time code has expired",
                        null)
                }
                "incorrect_code" -> {
                    listener.onError(
                        CardPinActionError.ONE_TIME_CODE_INCORRECT,
                        "The one-time code was incorrect",
                        null)
                }
                "too_many_attempts" -> {
                    listener.onError(
                        CardPinActionError.ONE_TIME_CODE_TOO_MANY_ATTEMPTS,
                        "The verification challenge was attempted too many times",
                        null)
                }
                "already_redeemed" -> {
                    listener.onError(
                        CardPinActionError.ONE_TIME_CODE_ALREADY_REDEEMED,
                        "The verification challenge was already redeemed",
                        null)
                }
                else -> {
                    listener.onError(
                        CardPinActionError.UNKNOWN_ERROR,
                        "The call to update the PIN failed, possibly an error " +
                            "with the verification. Please check the exception.",
                        e)
                }
            }
        } catch (e: APIConnectionException) {
            listener.onError(
                CardPinActionError.UNKNOWN_ERROR,
                "An error occurred retrieving the PIN please check the exception",
                e
            )
        } catch (e: APIException) {
            listener.onError(
                CardPinActionError.UNKNOWN_ERROR,
                "An error occurred retrieving the PIN please check the exception",
                e)
        } catch (e: AuthenticationException) {
            listener.onError(
                CardPinActionError.UNKNOWN_ERROR,
                "An error occurred retrieving the PIN please check the exception",
                e)
        } catch (e: CardException) {
            listener.onError(
                CardPinActionError.UNKNOWN_ERROR,
                "An error occurred retrieving the PIN please check the exception",
                e)
        }
    }

    private fun logMissingListener() {
        Log.e(TAG, "${this::class.java.name} was called without a listener")
    }

    enum class CardPinActionError {
        UNKNOWN_ERROR, EPHEMERAL_KEY_ERROR, ONE_TIME_CODE_INCORRECT,
        ONE_TIME_CODE_EXPIRED, ONE_TIME_CODE_TOO_MANY_ATTEMPTS,
        ONE_TIME_CODE_ALREADY_REDEEMED
    }

    interface IssuingCardPinRetrievalListener : Listener {
        fun onIssuingCardPinRetrieved(pin: String)
    }

    interface IssuingCardPinUpdateListener : Listener {
        fun onIssuingCardPinUpdated()
    }

    interface Listener {
        fun onError(
            errorCode: CardPinActionError,
            errorMessage: String?,
            exception: Throwable?
        )
    }

    companion object {
        private val TAG = IssuingCardPinService::class.java.name
        private const val KEY_REFRESH_BUFFER_IN_SECONDS = 30L

        /**
         * Create a IssuingCardPinService with the provided [EphemeralKeyProvider].
         *
         * @param keyProvider an [EphemeralKeyProvider] used to obtain an [EphemeralKey]
         */
        @JvmStatic
        fun create(
            context: Context,
            keyProvider: EphemeralKeyProvider
        ): IssuingCardPinService {
            return IssuingCardPinService(
                keyProvider,
                StripeApiRepository(context, appInfo),
                StripeOperationIdFactory()
            )
        }
    }
}
