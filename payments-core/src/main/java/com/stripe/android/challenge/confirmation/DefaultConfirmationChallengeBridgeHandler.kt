package com.stripe.android.challenge.confirmation

import android.webkit.JavascriptInterface
import com.stripe.android.core.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.json.JSONObject
import javax.inject.Inject

internal class DefaultConfirmationChallengeBridgeHandler @Inject constructor(
    private val args: IntentConfirmationChallengeArgs,
    private val logger: Logger
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
            val clientSecret = jsonObject.optString("client_secret")
                .takeIf { it.isNotBlank() }
                ?: args.intent.clientSecret
                ?: throw IllegalArgumentException("Missing client secret")

            _event.tryEmit(
                ConfirmationChallengeBridgeEvent.Success(clientSecret = clientSecret)
            )
        }.onFailure { error ->
            logger.error("Error parsing success response: ${error.message}", error)
            _event.tryEmit(
                ConfirmationChallengeBridgeEvent.Error(cause = error)
            )
        }
    }

    @JavascriptInterface
    override fun onError(errorMessage: String) {
        logMessage("Error from bridge: $errorMessage", emoji = "❌")
        _event.tryEmit(
            ConfirmationChallengeBridgeEvent.Error(
                cause = Exception(errorMessage)
            )
        )
    }

    @JavascriptInterface
    override fun logConsole(logData: String) {
        runCatching {
            val jsonObject = JSONObject(logData)
            val level = jsonObject.optString("level", "log")
            val message = jsonObject.optString("message", logData)

            val emoji = when (level.lowercase()) {
                "error" -> "❌"
                "warn" -> "⚠️"
                else -> "📝"
            }

            logMessage("Console ${level.uppercase()}: $message", emoji)
        }.onFailure {
            logMessage("Console: $logData")
        }
    }

    @JavascriptInterface
    override fun ready(message: String) {
        logMessage("Ready: $message")
    }

    private fun logMessage(message: String, emoji: String = "📝") {
        logger.debug("$emoji [ConfirmationChallenge] $message")
    }
}
