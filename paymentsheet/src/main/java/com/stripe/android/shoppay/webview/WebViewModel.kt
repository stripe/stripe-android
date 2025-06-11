package com.stripe.android.shoppay.webview

import android.util.Log
import android.webkit.WebView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.paymentsheet.PaymentRequestShippingContactUpdateHandler
import com.stripe.android.paymentsheet.WalletConfiguration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.BufferedReader
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
internal class WebViewModel(
    private val walletHandlers: WalletConfiguration.Handlers
) : ViewModel() {
    private val postMessageFilter = DefaultPostMessageFilter()
    val androidJsBridge = AndroidBridgeHelper(postMessageFilter)
    private val _webView = MutableStateFlow<WebView?>(null)
    val webView: StateFlow<WebView?> = _webView

    private val _popupWebView = MutableStateFlow<WebView?>(null)
    val popupWebView: StateFlow<WebView?> = _popupWebView

    val presentedWebView: StateFlow<WebView?> = webView.flatMapLatest { webView ->
        popupWebView.mapLatest { popup ->
            popup ?: webView
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _showPopup = MutableStateFlow(false)
    val showPopup: StateFlow<Boolean> = _showPopup

    init {
        listenForConfirmation()
    }
    private fun listenForConfirmation() {
        viewModelScope.launch {
            androidJsBridge.eventsFlow
                .filterIsInstance<ConfirmEvent>()
                .collect { event ->
                    handleConfirmEvent(event)
                }
        }

        viewModelScope.launch {
            androidJsBridge.eventsFlow
                .filterIsInstance<PingEvent>()
                .distinctUntilChanged()
                .collect { event ->
                    Log.d("WebViewModel", "Received PingEvent: ${event.currentFrame}")
                    Log.d("WebViewModel", "Received PingEvent: ${event.checkoutState}")
                }
        }
    }

    private fun handleConfirmEvent(event: ConfirmEvent) {
        val paymentDetails = event.paymentDetails ?: return
        
        // Handle shipping method updates if shipping address is present
        paymentDetails.shippingAddress?.let { shippingAddress ->
            walletHandlers.shippingContactUpdateHandler?.let { handler ->
                val selectedAddress = WalletConfiguration.SelectedPartialAddress(
                    city = shippingAddress.address?.city.orEmpty(),
                    state = shippingAddress.address?.state.orEmpty(),
                    postalCode = shippingAddress.address?.postalCode.orEmpty(),
                    country = "" // Country is not provided in the current data structure
                )
                
                // Create a callback that will be used to respond to the webview
                val callback = PaymentRequestShippingContactUpdateHandler {
//                    sendShippingContactUpdateToWebView(update)
                }
                
                handler(selectedAddress, callback)
            }
        }
        
        // Handle billing details if present
        paymentDetails.billingDetails?.let { billingDetails ->
            // Log billing details for debugging
            android.util.Log.d("WebViewModel", "Billing details received: name=${billingDetails.name}, email=${billingDetails.email}")
        }
        
        // Handle payment method type
        paymentDetails.paymentMethodType?.let { paymentMethodType ->
            android.util.Log.d("WebViewModel", "Payment method type: $paymentMethodType")
        }
        
        // Handle nonce (payment token)
        paymentDetails.nonce?.let { nonce ->
            android.util.Log.d("WebViewModel", "Payment nonce received: $nonce")
        }
    }
    
//    private fun sendShippingContactUpdateToWebView(update: WalletConfiguration.PaymentRequestShippingContactUpdate) {
//        val webView = _webView.value ?: return
//
//        // Convert the update to JSON and send to webview
//        val updateJson = buildString {
//            append("{")
//            append("\"lineItems\":[")
//            update.lineItems.forEachIndexed { index, item ->
//                if (index > 0) append(",")
//                append("{")
//                append("\"name\":\"${item.name}\",")
//                append("\"amount\":${item.amount}")
//                append("}")
//            }
//            append("],")
//            append("\"shippingRates\":[")
//            update.shippingRates.forEachIndexed { index, rate ->
//                if (index > 0) append(",")
//                append("{")
//                append("\"id\":\"${rate.id}\",")
//                append("\"amount\":${rate.amount},")
//                append("\"displayName\":\"${rate.displayName}\"")
//                rate.deliveryEstimate?.let { estimate ->
//                    append(",\"deliveryEstimate\":{")
//                    append("\"minimum\":{\"unit\":\"${estimate.minimum.unit.name.lowercase()}\",\"value\":${estimate.minimum.value}},")
//                    append("\"maximum\":{\"unit\":\"${estimate.maximum.unit.name.lowercase()}\",\"value\":${estimate.maximum.value}}")
//                    append("}")
//                }
//                append("}")
//            }
//            append("]")
//            append("}")
//        }
//
//        val jsCode = "window.updateShippingContactResponse && window.updateShippingContactResponse($updateJson);"
//        webView.evaluateJavascript(jsCode, null)
//    }

    fun setWebView(webView: WebView) {
        val htmlContent = webView.context.assets.open("index.html")
            .bufferedReader()
            .use(BufferedReader::readText)
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        _webView.value = webView
    }

    fun setPopupWebView(webView: WebView?) {
        _popupWebView.value = webView
        _showPopup.value = webView != null
    }

    fun closePopup() {
        _popupWebView.value = null
        _showPopup.value = false
    }

    fun injectJavaScriptBridge(webView: WebView?) {
        webView ?: return
        val jsBridge = webView.context.assets.open("native.js")
            .bufferedReader()
            .use(BufferedReader::readText)
//        val bridgeScript = """
//    // Override console.log to capture messages
//    (function() {
//        const originalLog = console.log;
//        console.log = function(...args) {
//            try {
//                window.androidBridge.consoleLog(
//                    'log',
//                    args.map(arg => typeof arg === 'object' ? JSON.stringify(arg) : String(arg)).join(' '),
//                    window.location.origin,
//                    window.location.href
//                );
//            } catch(e) {
//                // Ignore errors if androidBridge not available
//            }
//            originalLog.apply(console, arguments);
//        };
//
//        const originalError = console.error;
//        console.error = function(...args) {
//            try {
//                window.androidBridge.consoleLog(
//                    'error',
//                    args.map(arg => typeof arg === 'object' ? JSON.stringify(arg) : String(arg)).join(' '),
//                    window.location.origin,
//                    window.location.href
//                );
//            } catch(e) {
//                // Ignore errors if androidBridge not available
//            }
//            originalError.apply(console, arguments);
//        };
//
//        const originalWarn = console.warn;
//        console.warn = function(...args) {
//            try {
//                window.androidBridge.consoleLog(
//                    'warn',
//                    args.map(arg => typeof arg === 'object' ? JSON.stringify(arg) : String(arg)).join(' '),
//                    window.location.origin,
//                    window.location.href
//                );
//            } catch(e) {
//                // Ignore errors if androidBridge not available
//            }
//            originalWarn.apply(console, arguments);
//        };
//    })();
//
//    // Capture postMessage calls for the current frame only
//    (function() {
//        const originalPostMessage = window.postMessage;
//        window.postMessage = function(message, targetOrigin) {
//            try {
//                window.androidBridge.postMessage(JSON.stringify({
//                    type: 'postMessage',
//                    message: message,
//                    targetOrigin: targetOrigin,
//                    timestamp: Date.now(),
//                    origin: window.location.origin,
//                    url: window.location.href
//                }));
//            } catch(e) {
//                // Ignore errors if androidBridge not available
//            }
//            return originalPostMessage.call(window, message, targetOrigin);
//        };
//    })();
//
//    // Listen for message events (this captures both incoming and outgoing messages)
//    window.addEventListener('message', function(event) {
//        try {
//            // Capture detailed information about the message
//            let sourceInfo = 'unknown';
//            try {
//                if (event.source === window) {
//                    sourceInfo = 'self';
//                } else if (event.source === window.parent) {
//                    sourceInfo = 'parent';
//                } else if (event.source && event.source.location) {
//                    sourceInfo = event.source.location.origin;
//                } else {
//                    sourceInfo = 'iframe';
//                }
//            } catch(e) {
//                sourceInfo = 'cross-origin';
//            }
//
//            window.androidBridge.postMessage(JSON.stringify({
//                type: 'messageEvent',
//                data: event.data,
//                origin: event.origin,
//                source: sourceInfo,
//                timestamp: Date.now(),
//                currentFrame: window.location.href,
//                ports: event.ports ? event.ports.length : 0
//            }));
//        } catch(e) {
//            // Ignore errors if androidBridge not available
//        }
//    });
//
//    // Additional wallet-specific JavaScript bridge functions can be added here
//    if (typeof window.androidBridge !== 'undefined') {
//        // Expose shipping update functions to the web page
//        window.updateShippingMethod = function(name, rate) {
//            try {
//                window.androidBridge.updateShippingMethod(name, rate);
//            } catch(e) {
//                console.error('Failed to update shipping method:', e);
//            }
//        };
//
//        window.updateShippingContact = function(city, state, postalCode, country) {
//            try {
//                window.androidBridge.updateShippingContact(city, state, postalCode, country);
//            } catch(e) {
//                console.error('Failed to update shipping contact:', e);
//            }
//        };
//    }
//    """

        webView.evaluateJavascript(jsBridge, null)
    }

    private fun getEmojiForLogLevel(level: String): String {
        return when (level.lowercase()) {
            "error" -> "❌"
            "warning" -> "⚠️"
            "log" -> "📝"
            else -> "📝" // Default to log emoji
        }
    }

    class Factory(
        private val walletHandlers: WalletConfiguration.Handlers
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WebViewModel(walletHandlers) as T
        }
    }
}
