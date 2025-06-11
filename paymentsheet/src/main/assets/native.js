// Create native shipping API for index.html to use
window.NativeShipping = {
    // Promise-based API to calculate shipping
    calculateShipping: function(shippingAddress) {
        return new Promise((resolve, reject) => {
            const requestId = Date.now() + '_' + Math.random();

            // Store handlers for this request
            window.__shippingRequestHandlers = window.__shippingRequestHandlers || {};
            window.__shippingRequestHandlers[requestId] = { resolve, reject };

            // Send to native (Android)
            try {
                if (window.androidBridge && typeof window.androidBridge.calculateShipping === 'function') {
                    const payload = {
                        requestId: requestId,
                        shippingAddress: shippingAddress,
                        timestamp: Date.now()
                    };
                    window.androidBridge.calculateShipping(JSON.stringify(payload));
                } else {
                    reject(new Error('Android bridge or calculateShipping method not available.'));
                }
            } catch(e) {
                reject(new Error('Failed to communicate with native (Android): ' + e.message));
            }

            // Timeout after 10 seconds
            setTimeout(() => {
                if (window.__shippingRequestHandlers && window.__shippingRequestHandlers[requestId]) {
                    window.__shippingRequestHandlers[requestId].reject(new Error('Shipping calculation timed out'));
                    delete window.__shippingRequestHandlers[requestId];
                }
            }, 10000);
        });
    },

    // Called by native code (Android) to resolve/reject the promise
    // Native Android code would call this using:
    // webView.evaluateJavascript("javascript:window.NativeShipping.__handleResponse('" + requestId + "', " + successBoolean + ", " + JSON.stringify(dataObject) + ");", null);
    __handleResponse: function(requestId, success, data) {
        const handlers = window.__shippingRequestHandlers ? window.__shippingRequestHandlers[requestId] : undefined;
        if (!handlers) {
            console.warn('No handlers found for shipping request ID:', requestId);
            return;
        }

        if (success) {
            handlers.resolve(data);
        } else {
            // 'data' might be a string error message or an object with an 'error' property
            const errorMessage = (typeof data === 'object' && data && data.error) ? data.error : (data || 'Shipping calculation failed');
            handlers.reject(new Error(errorMessage));
        }

        delete window.__shippingRequestHandlers[requestId];
    }
};

console.log('Native Shipping API available at window.NativeShipping (for Android)');

// Notify that the bridge is ready (for Android)
try {
    if (window.androidBridge && typeof window.androidBridge.ready === 'function') {
        const readyPayload = {
            type: 'bridgeReady',
            timestamp: Date.now(),
            userAgent: navigator.userAgent,
            url: window.location.href,
            origin: window.location.origin,
            isTopFrame: window === window.top,
            nativeShippingAvailable: true
        };
        window.androidBridge.ready(JSON.stringify(readyPayload));
        console.log('Android bridge ready message sent.');
    } else {
        console.warn('Android bridge or ready method not available. Cannot send ready message.');
    }
} catch(e) {
    console.error('Error sending ready message to Android bridge:', e.message);
    // Ignore errors if androidBridge or its methods are not available
}

// Example of how you might set up the __shippingRequestHandlers initially if needed,
// though it's typically initialized on the first call to calculateShipping.
if (typeof window.__shippingRequestHandlers === 'undefined') {
    window.__shippingRequestHandlers = {};
}
