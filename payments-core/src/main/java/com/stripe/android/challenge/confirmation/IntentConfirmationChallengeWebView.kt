package com.stripe.android.challenge.confirmation

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.webkit.WebView

@SuppressLint("ViewConstructor")
internal class IntentConfirmationChallengeWebView(
    context: Context,
    bridgeHandler: ConfirmationChallengeBridgeHandler,
) : WebView(context) {

    init {
        setBackgroundColor(Color.TRANSPARENT)
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
        }
        addJavascriptInterface(bridgeHandler, "Android")
    }
}