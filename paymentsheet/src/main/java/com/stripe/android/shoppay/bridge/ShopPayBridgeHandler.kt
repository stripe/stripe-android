package com.stripe.android.shoppay.bridge

import android.webkit.JavascriptInterface
import kotlinx.coroutines.flow.StateFlow

internal interface ShopPayBridgeHandler {
    val confirmationState: StateFlow<ShopPayConfirmationState>

    @JavascriptInterface
    fun consoleLog(level: String, message: String, origin: String, url: String)

    @JavascriptInterface
    fun getStripePublishableKey(): String

    @JavascriptInterface
    fun handleECEClick(message: String): String

    @JavascriptInterface
    fun getShopPayInitParams(): String

    @JavascriptInterface
    fun calculateShipping(message: String): String?

    @JavascriptInterface
    fun calculateShippingRateChange(message: String): String?

    @JavascriptInterface
    fun confirmPayment(message: String): String

    @JavascriptInterface
    fun ready(message: String)
}
