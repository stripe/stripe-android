package com.stripe.android.shoppay.bridge

import android.webkit.JavascriptInterface

internal interface BridgeHandler {
    @JavascriptInterface
    fun consoleLog(level: String, message: String, origin: String, url: String)

    @JavascriptInterface
    fun getStripePublishableKey(): String

    @JavascriptInterface
    fun ready(message: String)
}
