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
                if (window.androidBridge && typeof window.androidBridge.logConsole === 'function') {
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

function parseNativeResponse(nativeResponseString) {
    try {
        const parsedResponse = typeof nativeResponseString === 'string' ?
            JSON.parse(nativeResponseString) : nativeResponseString;

        if (parsedResponse && parsedResponse.type === 'data') {
            return parsedResponse.data;
        } else if (parsedResponse && parsedResponse.type === 'error') {
            throw new Error(parsedResponse.message || 'Native call returned an error');
        } else {
            throw new Error(`Invalid response structure from native: ${JSON.stringify(parsedResponse)}`);
        }
    } catch (e) {
        if (e.message.includes('Invalid response structure') || e.message.includes('Native call returned an error')) {
            throw e;
        }
        throw new Error(`Failed to parse native response: ${e.message}`);
    }
}

function callNative(methodName, payload) {
    return new Promise((resolve, reject) => {
        try {
            // Add common fields to payload
            const requestPayload = payload ? {
                ...payload,
            } : {};

            if (window.androidBridge && typeof window.androidBridge[methodName] === 'function') {
                let nativeResponseString;
                if (payload) {
                    console.log(`Sending to Android bridge (${methodName}):`, JSON.stringify(requestPayload));
                    nativeResponseString = window.androidBridge[methodName](JSON.stringify(requestPayload));
                } else {
                    nativeResponseString = window.androidBridge[methodName]();
                }

                try {
                    const result = parseNativeResponse(nativeResponseString);
                    resolve(result);
                } catch (e) {
                    reject(e);
                }
                return;
            } else {
                reject(new Error(`Native bridge not available for method: ${methodName}`));
            }
        } catch (e) {
            console.error(`Error calling native method ${methodName}:`, e);
            reject(new Error(`Failed to communicate with native: ${e.message}`));
        }
    });
}

window.NativeStripeECE = {
    handleClick: function(eventData) {
        return callNative('handleECEClick', { eventData });
    },
    confirmPayment: function(paymentDetails) {
        return callNative('confirmPayment', { paymentDetails });
    },
    calculateShipping: function(shippingAddress) {
        return callNative('calculateShipping', { shippingAddress });
    },
    calculateShippingRateChange: function(shippingRate, currentAmount) {
        return callNative('calculateShippingRateChange', {
            shippingRate,
            currentAmount
        });
    },
    getStripePublishableKey: function() {
        const result = window.androidBridge['getStripePublishableKey']()
        console.log("result", result)
        return result
    },
    getShopPayInitParams: function() {
        try {
            if (window.androidBridge && typeof window.androidBridge.getShopPayInitParams === 'function') {
                const nativeResponseString = window.androidBridge.getShopPayInitParams();
                return parseNativeResponse(nativeResponseString);
            } else {
                throw new Error('Native bridge not available for method: getShopPayInitParams');
            }
        } catch (e) {
            console.error('Error calling native method getShopPayInitParams:', e);
            throw new Error(`Failed to communicate with native: ${e.message}`);
        }
    }
};

window.androidBridge.ready(JSON.stringify({
 type: 'bridgeReady'
}));
