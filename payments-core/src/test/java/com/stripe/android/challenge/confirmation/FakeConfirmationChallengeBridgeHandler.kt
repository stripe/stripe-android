package com.stripe.android.challenge.confirmation

import android.webkit.JavascriptInterface
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

internal class FakeConfirmationChallengeBridgeHandler : ConfirmationChallengeBridgeHandler {

    private val _event = MutableSharedFlow<ConfirmationChallengeBridgeEvent>(replay = 1)
    override val event: Flow<ConfirmationChallengeBridgeEvent> = _event

    suspend fun emitEvent(event: ConfirmationChallengeBridgeEvent) {
        _event.emit(event)
    }

    @JavascriptInterface
    override fun getInitParams() = "{}"

    @JavascriptInterface
    override fun onReady() = Unit

    @JavascriptInterface
    override fun onSuccess(paymentIntentJson: String) = Unit

    @JavascriptInterface
    override fun onError(errorMessage: String) = Unit
}
