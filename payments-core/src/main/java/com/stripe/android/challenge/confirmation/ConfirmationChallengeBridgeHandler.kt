package com.stripe.android.challenge.confirmation

import android.webkit.JavascriptInterface
import kotlinx.coroutines.flow.Flow

internal interface ConfirmationChallengeBridgeHandler {

    val event: Flow<ConfirmationChallengeBridgeEvent>

    @JavascriptInterface
    fun getInitParams(): String

    @JavascriptInterface
    fun onReady()

    @JavascriptInterface
    fun onSuccess(paymentIntentJson: String)

    @JavascriptInterface
    fun onError(errorMessage: String)
}
