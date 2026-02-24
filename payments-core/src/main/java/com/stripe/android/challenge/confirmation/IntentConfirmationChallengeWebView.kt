package com.stripe.android.challenge.confirmation

import android.content.Context
import android.graphics.Color
import android.webkit.WebView

internal open class IntentConfirmationChallengeWebView(context: Context) : WebView(context) {

    init {
        setBackgroundColor(Color.TRANSPARENT)
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
        }
    }

    open fun addBridgeHandler(handler: ConfirmationChallengeBridgeHandler) {
        addJavascriptInterface(handler, "Android")
    }

    open fun updateUserAgent(userAgent: String) {
        settings.apply {
            userAgentString = "${settings.userAgentString.orEmpty()} [$userAgent]"
        }
    }
}
