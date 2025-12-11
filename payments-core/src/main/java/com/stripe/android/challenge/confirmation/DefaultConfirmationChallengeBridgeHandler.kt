package com.stripe.android.challenge.confirmation

import android.webkit.JavascriptInterface
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.analytics.ErrorReporter.UnexpectedErrorEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.json.JSONObject
import javax.inject.Inject

internal class DefaultConfirmationChallengeBridgeHandler @Inject constructor(
    private val successParamsParser: ModelJsonParser<BridgeSuccessParams>,
    private val errorParamsParser: ModelJsonParser<BridgeErrorParams>,
    private val args: IntentConfirmationChallengeArgs,
    private val logger: Logger,
    private val errorReporter: ErrorReporter,
) : ConfirmationChallengeBridgeHandler {

    private val _event = MutableSharedFlow<ConfirmationChallengeBridgeEvent>(replay = 1)
    override val event: Flow<ConfirmationChallengeBridgeEvent> = _event

    @JavascriptInterface
    override fun getInitParams(): String {
        val initParams = JSONObject().apply {
            put("publishableKey", args.publishableKey)
            put("clientSecret", args.intent.clientSecret)
        }
        logMessage("Returning init params: $initParams")
        return initParams.toString()
    }

    @JavascriptInterface
    override fun onReady() {
        logMessage("Bridge is ready")
        _event.tryEmit(ConfirmationChallengeBridgeEvent.Ready)
    }

    @JavascriptInterface
    override fun onSuccess(paymentIntentJson: String) {
        logMessage("Payment intent success: $paymentIntentJson")
        runCatching {
            val jsonObject = JSONObject(paymentIntentJson)
            val successParams = successParamsParser.parse(jsonObject)
            val clientSecret = successParams?.clientSecret
                ?: args.intent.clientSecret
                ?: throw IllegalArgumentException("Missing client secret")
            _event.tryEmit(
                ConfirmationChallengeBridgeEvent.Success(clientSecret)
            )
        }.onFailure { error ->
            errorReporter.report(
                UnexpectedErrorEvent.INTENT_CONFIRMATION_CHALLENGE_FAILED_TO_PARSE_SUCCESS_CALLBACK_PARAMS,
                stripeException = StripeException.create(error)
            )
            _event.tryEmit(ConfirmationChallengeBridgeEvent.Error(error))
        }
    }

    @JavascriptInterface
    override fun onError(errorMessage: String) {
        logMessage("Error from bridge: $errorMessage")
        runCatching {
            val jsonObject = JSONObject(errorMessage)
            val errorParams = errorParamsParser.parse(jsonObject)
            val bridgeError = BridgeError(
                message = errorParams?.message,
                type = errorParams?.type,
                code = errorParams?.code
            )
            _event.tryEmit(ConfirmationChallengeBridgeEvent.Error(bridgeError))
        }.onFailure { error ->
            errorReporter.report(
                UnexpectedErrorEvent.INTENT_CONFIRMATION_CHALLENGE_FAILED_TO_PARSE_ERROR_CALLBACK_PARAMS,
                stripeException = StripeException.create(error)
            )
            _event.tryEmit(
                ConfirmationChallengeBridgeEvent.Error(error)
            )
        }
    }

    private fun logMessage(message: String) {
        logger.debug("[ConfirmationChallenge] $message")
    }
}
