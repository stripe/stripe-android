package com.stripe.android.challenge.confirmation

import android.content.Context
import app.cash.turbine.Turbine

internal class FakeIntentConfirmationChallengeWebView(
    context: Context,
) : IntentConfirmationChallengeWebView(context) {

    private val calls = Turbine<Call>()

    override fun loadUrl(url: String) {
        calls.add(Call.LoadUrl(url))
    }

    override fun addBridgeHandler(handler: ConfirmationChallengeBridgeHandler) {
        calls.add(Call.AddBridgeHandler(handler))
    }

    suspend fun awaitCall() = calls.awaitItem()

    fun ensureAllEventsConsumed() = calls.ensureAllEventsConsumed()

    sealed interface Call {
        data class LoadUrl(val url: String) : Call
        data class AddBridgeHandler(val handler: ConfirmationChallengeBridgeHandler) : Call
    }
}
