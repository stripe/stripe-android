package com.stripe.android.challenge.confirmation

import android.content.Context
import android.webkit.WebViewClient
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

    override fun setWebViewClient(client: WebViewClient) {
        calls.add(Call.SetWebViewClient(client))
    }

    suspend fun awaitCall() = calls.awaitItem()

    fun ensureAllEventsConsumed() = calls.ensureAllEventsConsumed()

    sealed interface Call {
        data class LoadUrl(val url: String) : Call
        data class AddBridgeHandler(val handler: ConfirmationChallengeBridgeHandler) : Call
        data class SetWebViewClient(val webViewClient: WebViewClient) : Call
    }
}
