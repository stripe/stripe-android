package com.stripe.android.shoppay.webview

import android.util.Log
import android.webkit.JavascriptInterface
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AndroidBridgeHelper(
    private val postMessageFilter: PostMessageFilter
) {
    private val _eventsFlow = MutableSharedFlow<StripeParentEvent>()
    val eventsFlow: Flow<StripeParentEvent> = _eventsFlow
    // Initialize SimpleDateFormat once per instance for efficiency
    private val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    private val timestampProvider: () -> String = {
        sdf.format(Date()) // Date() is new each time, which is correct for a timestamp
    }

    @JavascriptInterface
    fun postMessage(message: String) {
        Log.d("WebViewBridge", "📨 [${timestampProvider()}] Stripe Message Bridge: $message")
        postMessageFilter.filter(message)
            ?.let {
                Log.d("WebViewBridge", "📨 [${timestampProvider()}] Stripe Parent Event: $it")
                _eventsFlow.tryEmit(it)
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
        Log.d(
            "WebViewBridge",
            "$emoji [${timestampProvider()}] Console ${level.uppercase()}$frameInfo: $message"
        )
        if (message.lowercase().contains("stripe") || message.lowercase().contains("error")) {
            Log.d("WebViewBridge", "   📍 Source: $url")
        }
    }

    @JavascriptInterface
    fun ready(userAgent: String, url: String, origin: String, isTopFrame: Boolean) {
        Log.d("WebViewBridge", "✅ [${timestampProvider()}] Bridge Ready:")
        if (userAgent.contains("Safari") && !userAgent.contains("Mobile")) {
            Log.d("WebViewBridge", "   🎯 Safari User Agent Detected: SUCCESS")
        } else if (userAgent.contains("Safari")) {
            Log.d("WebViewBridge", "   📱 Mobile Safari User Agent Detected: SUCCESS")
        } else {
            Log.d("WebViewBridge", "   ⚠️ WebView User Agent Detected (not Safari)")
        }
        Log.d("WebViewBridge", "   userAgent: $userAgent")
        Log.d("WebViewBridge", "   url: $url")
        Log.d("WebViewBridge", "   origin: $origin")
        Log.d("WebViewBridge", "   isTopFrame: $isTopFrame")
    }

    @JavascriptInterface
    fun error(message: String) {
        Log.d("WebViewBridge", "❌ [${timestampProvider()}] Bridge Error: $message")
    }
}