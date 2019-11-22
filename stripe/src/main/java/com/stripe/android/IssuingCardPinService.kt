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
) : KeyManagerListener {

    private val retrievalListeners: MutableMap<String?, IssuingCardPinRetrievalListener> =
        mutableMapOf()
    private val updateListeners: MutableMap<String?, IssuingCardPinUpdateListener> =
        mutableMapOf()
    private val ephemeralKeyManager = EphemeralKeyManager(
        keyProvider,
        this,
        KEY_REFRESH_BUFFER_IN_SECONDS,
        null,
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
        val arguments = mapOf(
            ARGUMENT_CARD_ID to cardId,
            ARGUMENT_VERIFICATION_ID to verificationId,
            ARGUMENT_ONE_TIME_CODE to userOneTimeCode
        )
        val operationId = operationIdFactory.create()
        retrievalListeners[operationId] = listener
        ephemeralKeyManager.retrieveEphemeralKey(operationId, PIN_RETRIEVE, arguments)
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
        val arguments = mapOf(
            ARGUMENT_CARD_ID to cardId,
            ARGUMENT_NEW_PIN to newPin,
            ARGUMENT_VERIFICATION_ID to verificationId,
            ARGUMENT_ONE_TIME_CODE to userOneTimeCode
        )
        val operationId = operationIdFactory.create()
        updateListeners[operationId] = listener
        ephemeralKeyManager.retrieveEphemeralKey(operationId, PIN_UPDATE, arguments)
    }

    override fun onKeyUpdate(
        ephemeralKey: EphemeralKey,
        operationId: String,
        action: String?,
        arguments: Map<String, Any>?
    ) {
        when (action) {
            PIN_RETRIEVE -> fireRetrievePinRequest(ephemeralKey, operationId, arguments)
            PIN_UPDATE -> fireUpdatePinRequest(ephemeralKey, operationId, arguments)
        }
    }

    private fun fireRetrievePinRequest(
        ephemeralKey: EphemeralKey,
        operationId: String,
        arguments: Map<String, Any>?
    ) {
        val listener = retrievalListeners.remove(operationId)
        if (listener == null) {
            Log.e(TAG, IssuingCardPinService::class.java.name +
                " was called without a listener")
            return
        }
        if (arguments == null) {
            listener.onError(
                CardPinActionError.UNKNOWN_ERROR,
                "Arguments were lost during the ephemeral key call, " +
                    "this is not supposed to happen," +
                    " please contact support@stripe.com for assistance.",
                null)
            return
        }
        val cardId = requireNotNull(arguments[ARGUMENT_CARD_ID]) as String
        val verificationId = requireNotNull(arguments[ARGUMENT_VERIFICATION_ID]) as String
        val userOneTimeCode = requireNotNull(arguments[ARGUMENT_ONE_TIME_CODE]) as String
        try {
            val pin = stripeRepository.retrieveIssuingCardPin(cardId, verificationId,
                userOneTimeCode, ephemeralKey.secret)
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
        operationId: String,
        arguments: Map<String, Any>?
    ) {
        val listener = updateListeners.remove(operationId)
        if (listener == null) {
            Log.e(TAG, IssuingCardPinService::class.java.name +
                " was called without a listener")
            return
        }
        if (arguments == null) {
            listener.onError(
                CardPinActionError.UNKNOWN_ERROR,
                "Arguments were lost during the ephemeral key call, " +
                    "this is not supposed to happen," +
                    " please contact support@stripe.com for assistance.",
                null
            )
            return
        }
        val cardId = requireNotNull(arguments[ARGUMENT_CARD_ID]) as String
        val newPin = requireNotNull(arguments[ARGUMENT_NEW_PIN]) as String
        val verificationId = requireNotNull(arguments[ARGUMENT_VERIFICATION_ID]) as String
        val userOneTimeCode = requireNotNull(arguments[ARGUMENT_ONE_TIME_CODE]) as String
        try {
            stripeRepository.updateIssuingCardPin(cardId, newPin, verificationId,
                userOneTimeCode, ephemeralKey.secret)
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

    enum class CardPinActionError {
        UNKNOWN_ERROR, EPHEMERAL_KEY_ERROR, ONE_TIME_CODE_INCORRECT,
        ONE_TIME_CODE_EXPIRED, ONE_TIME_CODE_TOO_MANY_ATTEMPTS,
        ONE_TIME_CODE_ALREADY_REDEEMED
    }

    interface IssuingCardPinRetrievalListener {
        fun onIssuingCardPinRetrieved(pin: String)

        fun onError(
            errorCode: CardPinActionError,
            errorMessage: String?,
            exception: Throwable?
        )
    }

    interface IssuingCardPinUpdateListener {
        fun onIssuingCardPinUpdated()

        fun onError(
            errorCode: CardPinActionError,
            errorMessage: String?,
            exception: Throwable?
        )
    }

    companion object {
        private val TAG = IssuingCardPinService::class.java.name
        private const val KEY_REFRESH_BUFFER_IN_SECONDS = 30L

        private const val PIN_RETRIEVE = "PIN_RETRIEVE"
        private const val PIN_UPDATE = "PIN_UPDATE"

        private const val ARGUMENT_CARD_ID = "cardId"
        private const val ARGUMENT_VERIFICATION_ID = "verificationId"
        private const val ARGUMENT_ONE_TIME_CODE = "userOneTimeCode"
        private const val ARGUMENT_NEW_PIN = "newPin"

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
