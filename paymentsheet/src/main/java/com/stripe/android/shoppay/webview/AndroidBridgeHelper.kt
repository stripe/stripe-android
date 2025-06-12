package com.stripe.android.shoppay.webview

import android.util.Log
import android.webkit.JavascriptInterface
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentsheet.FLOW_CONTROLLER_DEFAULT_CALLBACK_IDENTIFIER
import com.stripe.android.paymentsheet.WalletConfiguration
import com.stripe.android.shoppay.bridge.BridgeResponse
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AndroidBridgeHelper(
    private val postMessageFilter: PostMessageFilter
) {
    private val _eventsFlow = MutableSharedFlow<StripeParentEvent>()
    val eventsFlow: Flow<StripeParentEvent> = _eventsFlow

    // Initialize SimpleDateFormat once per instance for efficiency
    private val timestampProvider: () -> String = {
        SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
    }

    private val shippingCalculationRequestParser = ShippingCalculationRequestJsonParser()
    private val shippingRateChangeRequestParser = ShippingRateChangeRequestJsonParser()

    private val walletHandlers: WalletConfiguration.Handlers?
        get() = PaymentElementCallbackReferences[FLOW_CONTROLLER_DEFAULT_CALLBACK_IDENTIFIER]?.walletHandlers

    @JavascriptInterface
    fun postMessage(message: String) {
        if (!message.contains("ping")) {
            logMessage("📨", "Stripe Message Bridge: $message")
        }
        postMessageFilter.filter(message)?.let {
            GlobalScope.launch {
                _eventsFlow.emit(it)
            }
        }
    }

    @JavascriptInterface
    fun ready(message: String) {
        // No-op method for compatibility
    }

    @JavascriptInterface
    fun calculateShipping(message: String): String {
        logMessage("📦", "Shipping calculation request: $message")

        return handleShippingRequest(message) { jsonObject ->
            val shippingRequest = shippingCalculationRequestParser.parse(jsonObject)
                ?: return@handleShippingRequest createErrorResponse("Failed to parse shipping rate change request")

            val address = WalletConfiguration.SelectedPartialAddress(
                city = shippingRequest.shippingAddress.city.orEmpty(),
                state = shippingRequest.shippingAddress.provinceCode.orEmpty(),
                postalCode = shippingRequest.shippingAddress.postalCode.orEmpty(),
                country = shippingRequest.shippingAddress.countryCode.orEmpty()
            )

            processWalletHandlerUpdate { handlers, continuation ->
                handlers.shippingContactUpdateHandler?.invoke(address) { update ->
                    val response = ShippingResponse(
                        merchantDecision = "accepted",
                        lineItems = update.lineItems,
                        shippingRates = update.shippingRates,
                        totalAmount = 500
                    )
                    continuation.resume(wrapInBridgeResponse(response))
                } ?: continuation.resume(createErrorResponse("Failed to parse shipping rate change request"))
            }
        }
    }

    @JavascriptInterface
    fun calculateShippingRateChange(message: String): String {
        logMessage("📦", "Shipping rate change request: $message")

        return handleShippingRequest(message) { jsonObject ->
            val shippingRateChangeRequest = shippingRateChangeRequestParser.parse(jsonObject)
                ?: return@handleShippingRequest createErrorResponse("Failed to parse shipping rate change request")

            val selectedShippingRate = WalletConfiguration.SelectedShippingRate(
                name = shippingRateChangeRequest.shippingRate.displayName,
                rate = shippingRateChangeRequest.shippingRate.id
            )

            processWalletHandlerUpdate { handlers, continuation ->
                handlers.shippingMethodUpdateHandler?.invoke(selectedShippingRate) { update ->
                    val response = ShippingResponse(
                        merchantDecision = "accepted",
                        lineItems = update.lineItems,
                        shippingRates = update.shippingRates,
                        totalAmount = null
                    )
                    continuation.resume(wrapInBridgeResponse(response))
                } ?: continuation.resume(createErrorResponse("Failed to parse shipping rate change request"))
            }
        }
    }

    @JavascriptInterface
    fun consoleLog(level: String, message: String, origin: String, url: String) {
        val emoji = when (level.lowercase()) {
            "error" -> "❌"
            "warn" -> "⚠️"
            else -> "📝"
        }
        val frameInfo = if (origin != url) "[$origin]" else ""
        logMessage(emoji, "Console ${level.uppercase()}$frameInfo: $message")

        if (message.lowercase().contains("stripe") || message.lowercase().contains("error")) {
            Log.d("WebViewBridge", "   📍 Source: $url")
        }
    }

    @JavascriptInterface
    fun ready(userAgent: String, url: String, origin: String, isTopFrame: Boolean) {
        logMessage("✅", "Bridge Ready:")

        val userAgentInfo = when {
            userAgent.contains("Safari") && !userAgent.contains("Mobile") ->
                "   🎯 Safari User Agent Detected: SUCCESS"
            userAgent.contains("Safari") ->
                "   📱 Mobile Safari User Agent Detected: SUCCESS"
            else ->
                "   ⚠️ WebView User Agent Detected (not Safari)"
        }

        Log.d("WebViewBridge", userAgentInfo)
        Log.d("WebViewBridge", "   userAgent: $userAgent")
        Log.d("WebViewBridge", "   url: $url")
        Log.d("WebViewBridge", "   origin: $origin")
        Log.d("WebViewBridge", "   isTopFrame: $isTopFrame")
    }

    @JavascriptInterface
    fun error(message: String) {
        logMessage("❌", "Bridge Error: $message")
    }

    /**
     * Helper function for consistent logging
     */
    private fun logMessage(emoji: String, message: String) {
        Log.d("WebViewBridge", "$emoji [${timestampProvider()}] $message")
    }

    /**
     * Generic handler for shipping-related requests
     */
    private inline fun handleShippingRequest(
        message: String,
        crossinline requestHandler: (JSONObject) -> String
    ): String {
        return try {
            val jsonObject = JSONObject(message)
            requestHandler(jsonObject)
        } catch (e: Exception) {
            Log.e("WebViewBridge", "❌ Error parsing shipping request: ${e.message}", e)
            createErrorResponse("❌ Error parsing shipping request: ${e.message}")
        }
    }

    /**
     * Generic processor for wallet handler updates
     */
    private fun processWalletHandlerUpdate(
        handlerFunction: (WalletConfiguration.Handlers, kotlin.coroutines.Continuation<String>) -> Unit
    ): String {
        val handlers = walletHandlers ?: return createErrorResponse("Failed to parse shipping rate change request")

        return runBlocking {
            suspendCoroutine<String> { continuation ->
                handlerFunction(handlers, continuation)
            }
        }
    }

    /**
     * Creates a standardized error response
     */
    private fun createErrorResponse(message: String): String {
        return BridgeResponse.Error<ShippingResponse>(message = message).toJson().toString()
    }

    /**
     * Wraps a response in a BridgeResponse.Data and converts to JSON string
     */
    private fun wrapInBridgeResponse(response: ShippingResponse): String {
        return BridgeResponse.Data(data = response).toJson().toString()
    }
}
