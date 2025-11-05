package com.stripe.android.challenge.confirmation

import android.webkit.JavascriptInterface
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

internal class FakeConfirmationChallengeBridgeHandler : ConfirmationChallengeBridgeHandler {

    private val _event = MutableSharedFlow<ConfirmationChallengeBridgeEvent>(replay = 1)
    override val event: Flow<ConfirmationChallengeBridgeEvent> = _event

    var initParamsResult: String = "{}"
    var onReadyCalled: Boolean = false
    var onSuccessCalled: Boolean = false
    var lastSuccessPaymentIntentJson: String? = null
    var onErrorCalled: Boolean = false
    var lastErrorMessage: String? = null
    var logConsoleCalled: Boolean = false
    var lastLogData: String? = null
    var readyCalled: Boolean = false
    var lastReadyMessage: String? = null

    suspend fun emitEvent(event: ConfirmationChallengeBridgeEvent) {
        _event.emit(event)
    }

    @JavascriptInterface
    override fun getInitParams(): String {
        return initParamsResult
    }

    @JavascriptInterface
    override fun onReady() {
        onReadyCalled = true
    }

    @JavascriptInterface
    override fun onSuccess(paymentIntentJson: String) {
        onSuccessCalled = true
        lastSuccessPaymentIntentJson = paymentIntentJson
    }

    @JavascriptInterface
    override fun onError(errorMessage: String) {
        onErrorCalled = true
        lastErrorMessage = errorMessage
    }

    @JavascriptInterface
    override fun logConsole(logData: String) {
        logConsoleCalled = true
        lastLogData = logData
    }

    @JavascriptInterface
    override fun ready(message: String) {
        readyCalled = true
        lastReadyMessage = message
    }

    fun reset() {
        onReadyCalled = false
        onSuccessCalled = false
        lastSuccessPaymentIntentJson = null
        onErrorCalled = false
        lastErrorMessage = null
        logConsoleCalled = false
        lastLogData = null
        readyCalled = false
        lastReadyMessage = null
    }
}
