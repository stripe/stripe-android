// Helper to generate a unique request ID
function generateRequestId() {
    return 'req_' + Date.now() + '_' + Math.random().toString(36).substring(2, 9);
}

// Unified console interceptor to log to native side
(function() {
    const originalConsole = {
        log: console.log,
        error: console.error,
        warn: console.warn,
        info: console.info,
        debug: console.debug
    };

    function formatArgs(args) {
        return Array.from(args).map(arg => {
            if (typeof arg === 'object') {
                try {
                    return JSON.stringify(arg, null, 2);
                } catch (e) {
                    return String(arg);
                }
            }
            return String(arg);
        }).join(' ');
    }

    ['log', 'error', 'warn', 'info', 'debug'].forEach(method => {
        console[method] = function(...args) {
            originalConsole[method].apply(console, args);
            try {
                if (window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers.consoleLog) {
                    // iOS logging
                    window.webkit.messageHandlers.consoleLog.postMessage({
                        level: method,
                        message: formatArgs(args),
                        timestamp: Date.now(),
                        stackTrace: method === 'error' ? (new Error()).stack : undefined
                    });
                } else if (window.androidBridge && typeof window.androidBridge.logConsole === 'function') {
                    // Android logging
                    window.androidBridge.logConsole(JSON.stringify({
                        level: method,
                        message: formatArgs(args),
                        timestamp: Date.now(),
                        stackTrace: method === 'error' ? (new Error()).stack : undefined
                    }));
                }
            } catch(e) {}
        };
    });
})();

// Function to call native methods that works on both platforms
function callNative(methodName, payload) {
    return new Promise((resolve, reject) => {
        try {
            // Add common fields to payload
            const requestPayload = {
                ...payload,
                timestamp: Date.now(),
                requestId: generateRequestId()
            };

            // iOS implementation
            if (window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers[methodName]) {
                window.webkit.messageHandlers[methodName].postMessage(requestPayload)
                    .then(response => resolve(response))
                    .catch(error => reject(new Error(`iOS native error: ${error.message || error}`)));
                return;
            }

            // Android implementation
            if (window.androidBridge && typeof window.androidBridge[methodName] === 'function') {
                console.log(`Sending to Android bridge (${methodName}):`, JSON.stringify(requestPayload));
                const nativeResponseString = window.androidBridge[methodName](JSON.stringify(requestPayload));

                try {
                    const parsedResponse = typeof nativeResponseString === 'string' ?
                        JSON.parse(nativeResponseString) : nativeResponseString;

                    if (parsedResponse && parsedResponse.type === 'data') {
                        resolve(parsedResponse.data);
                    } else if (parsedResponse && parsedResponse.type === 'error') {
                        reject(new Error(parsedResponse.message || 'Native call returned an error'));
                    } else {
                        reject(new Error(`Invalid response structure from native: ${JSON.stringify(parsedResponse)}`));
                    }
                } catch (e) {
                    reject(new Error(`Failed to parse native response: ${e.message}`));
                }
                return;
            }

            // Neither Android nor iOS bridge available
            reject(new Error(`Native bridge not available for method: ${methodName}`));
        } catch (e) {
            console.error(`Error calling native method ${methodName}:`, e);
            reject(new Error(`Failed to communicate with native: ${e.message}`));
        }
    });
}

// Unified Shipping API
window.NativeShipping = {
    calculateShipping: function(shippingAddress) {
        return callNative('calculateShipping', { shippingAddress });
    },

    calculateShippingRateChange: function(shippingRate, currentAmount) {
        return callNative('calculateShippingRateChange', {
            shippingRate,
            currentAmount
        });
    }
};

// Unified ECE API
window.NativeECE = {
    handleClick: function(eventData) {
        return callNative('handleECEClick', { eventData });
    }
};

// Unified Payment API
window.NativePayment = {
    confirmPayment: function(paymentDetails) {
        return callNative('confirmPayment', { paymentDetails });
    }
};

// Send ready notification to bridge
(function notifyBridgeReady() {
    try {
        const readyPayload = {
            type: 'bridgeReady',
            timestamp: Date.now(),
            userAgent: navigator.userAgent,
            url: window.location.href,
            origin: window.location.origin,
            isTopFrame: window === window.top,
            nativeShippingAvailable: true
        };

        // For iOS
        if (window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers.ready) {
            window.webkit.messageHandlers.ready.postMessage(readyPayload);
            console.log('iOS bridge ready message sent.');
        }

        // For Android
        if (window.androidBridge && typeof window.androidBridge.ready === 'function') {
            window.androidBridge.ready(JSON.stringify(readyPayload));
            console.log('Android bridge ready message sent.');
        }

        if (!window.webkit?.messageHandlers?.ready && !window.androidBridge?.ready) {
            console.warn('Neither iOS nor Android bridge available. Cannot send ready message.');
        }
    } catch(e) {
        console.error('Error sending ready message to native bridge:', e.message);
    }
})();

console.log('Native APIs available at window.NativeShipping, window.NativeECE, and window.NativePayment');
