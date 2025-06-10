package com.stripe.android.shoppay.webview

import android.webkit.WebView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
internal class WebViewModel : ViewModel() {
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

    fun setWebView(webView: WebView) {
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
        val bridgeScript = """
    // Override console.log to capture messages
    (function() {
        const originalLog = console.log;
        console.log = function(...args) {
            try {
                window.androidBridge.consoleLog(
                    'log',
                    args.map(arg => typeof arg === 'object' ? JSON.stringify(arg) : String(arg)).join(' '),
                    window.location.origin,
                    window.location.href
                );
            } catch(e) {
                // Ignore errors if androidBridge not available
            }
            originalLog.apply(console, arguments);
        };
        
        const originalError = console.error;
        console.error = function(...args) {
            try {
                window.androidBridge.consoleLog(
                    'error', 
                    args.map(arg => typeof arg === 'object' ? JSON.stringify(arg) : String(arg)).join(' '),
                    window.location.origin,
                    window.location.href
                );
            } catch(e) {
                // Ignore errors if androidBridge not available
            }
            originalError.apply(console, arguments);
        };
        
        const originalWarn = console.warn;
        console.warn = function(...args) {
            try {
                window.androidBridge.consoleLog(
                    'warn',
                    args.map(arg => typeof arg === 'object' ? JSON.stringify(arg) : String(arg)).join(' '),
                    window.location.origin,
                    window.location.href
                );
            } catch(e) {
                // Ignore errors if androidBridge not available
            }
            originalWarn.apply(console, arguments);
        };
    })();
    
    // Capture postMessage calls for the current frame only
    (function() {
        const originalPostMessage = window.postMessage;
        window.postMessage = function(message, targetOrigin) {
            try {
                window.androidBridge.postMessage(JSON.stringify({
                    type: 'postMessage',
                    message: message,
                    targetOrigin: targetOrigin,
                    timestamp: Date.now(),
                    origin: window.location.origin,
                    url: window.location.href
                }));
            } catch(e) {
                // Ignore errors if androidBridge not available
            }
            return originalPostMessage.call(window, message, targetOrigin);
        };
    })();
    
    // Listen for message events (this captures both incoming and outgoing messages)
    window.addEventListener('message', function(event) {
        try {
            // Capture detailed information about the message
            let sourceInfo = 'unknown';
            try {
                if (event.source === window) {
                    sourceInfo = 'self';
                } else if (event.source === window.parent) {
                    sourceInfo = 'parent';
                } else if (event.source && event.source.location) {
                    sourceInfo = event.source.location.origin;
                } else {
                    sourceInfo = 'iframe';
                }
            } catch(e) {
                sourceInfo = 'cross-origin';
            }
            
            window.androidBridge.postMessage(JSON.stringify({
                type: 'messageEvent',
                data: event.data,
                origin: event.origin,
                source: sourceInfo,
                timestamp: Date.now(),
                currentFrame: window.location.href,
                ports: event.ports ? event.ports.length : 0
            }));
        } catch(e) {
            // Ignore errors if androidBridge not available
        }
    });
    
    // Enhanced detection for Stripe-specific communications
    (function() {
        // Override XHR
        const originalSend = XMLHttpRequest.prototype.send;
        XMLHttpRequest.prototype.send = function(...args) {
            try {
                window.androidBridge.postMessage(JSON.stringify({
                    type: 'xhr',
                    url: this.responseURL || 'unknown',
                    method: this._method || 'unknown',
                    timestamp: Date.now(),
                    origin: window.location.origin
                }));
            } catch(e) {
                // Ignore errors
            }
            return originalSend.apply(this, args);
        };
        
        const originalOpen = XMLHttpRequest.prototype.open;
        XMLHttpRequest.prototype.open = function(method, ...args) {
            this._method = method;
            return originalOpen.apply(this, [method, ...args]);
        };
        
        // Monitor fetch requests
        if (window.fetch) {
            const originalFetch = window.fetch;
            window.fetch = function(...args) {
                try {
                    const url = args[0];
                    window.androidBridge.postMessage(JSON.stringify({
                        type: 'fetch',
                        url: typeof url === 'string' ? url : url.url,
                        timestamp: Date.now(),
                        origin: window.location.origin
                    }));
                } catch(e) {
                    // Ignore errors
                }
                return originalFetch.apply(this, args);
            };
        }
    })();
    
    // Notify that the bridge is ready
    try {
        window.androidBridge.ready(
            navigator.userAgent,
            window.location.href,
            window.location.origin,
            window === window.top
        );
    } catch(e) {
        window.androidBridge.error('Bridge initialization failed: ' + e.message);
    }
    """

        webView?.evaluateJavascript(bridgeScript, null)
    }

    private fun getEmojiForLogLevel(level: String): String {
        return when (level.lowercase()) {
            "error" -> "❌"
            "warning" -> "⚠️"
            "log" -> "📝"
            else -> "📝" // Default to log emoji
        }
    }
}
