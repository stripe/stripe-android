// Helper to generate a unique request ID
function generateRequestId() {
    return 'req_' + Date.now() + '_' + Math.random().toString(36).substring(2, 9);
}

// Helper function to make native bridge calls
    function callNativeBridge(methodName, payload) {
        try {
            if (window.androidBridge && typeof window.androidBridge[methodName] === 'function') {
                const requestPayload = {
                    ...payload,
                    timestamp: Date.now()
                };

                // Add request ID if not already present
                if (!requestPayload.requestId) {
                    requestPayload.requestId = generateRequestId();
                }

                return promiseWrapper(() => {
                    console.log(`Sending to native bridge (${methodName}):`, JSON.stringify(requestPayload));
                    const nativeResponseString = window.androidBridge[methodName](JSON.stringify(requestPayload));
                    console.log('Raw response from native bridge:', nativeResponseString);
                    return parseNativeResponse(nativeResponseString);
                });
            } else {
                return Promise.reject(new Error(`Android bridge or ${methodName} method not available.`));
            }
        } catch(e) {
            console.error(`Error in ${methodName} before/during native call:`, e);
            return Promise.reject(new Error(`Failed to communicate with native (Android): ${e.message}`));
        }
    }

// Create native shipping API for index.html to use
window.NativeShipping = (function() {
    // API implementation
    return {
        calculateShipping: function(shippingAddress) {
            return callNativeBridge('calculateShipping', { shippingAddress });
        },

        calculateShippingRateChange: function(shippingRate, currentAmount) {
            return callNativeBridge('calculateShippingRateChange', {
                shippingRate,
                currentAmount
            });
        }
    };
})();

window.NativeECE = (function() {
   // API implementation
   return {
       handleClick: function(eventData) {
           return callNativeBridge('handleECEClick', { eventData });
       }
   };
})();

window.NativePayment = (function() {
   // API implementation
   return {
       confirmPayment: function(paymentDetails) {
           return callNativeBridge('confirmPayment', { paymentDetails });
       }
   };
})();

function promiseWrapper(block) {
    return new Promise((resolve, reject) => {
        try {
            const resultFromBlock = block();
            console.log('Parsed result from block for promiseWrapper:', resultFromBlock);

            if (resultFromBlock && typeof resultFromBlock === 'object') {
                if (resultFromBlock.type === 'data') {
                    if (resultFromBlock.data !== undefined) {
                        resolve(resultFromBlock.data);
                    } else {
                        reject(new Error('Native result type is "data" but "data" field is missing or undefined.'));
                    }
                } else if (resultFromBlock.type === 'error') {
                    reject(new Error(resultFromBlock.message || 'Native call returned an error object.'));
                } else {
                    reject(new Error(`Invalid result type: ${resultFromBlock.type}. Expected "data" or "error".`));
                }
            } else {
                reject(new Error('Invalid result structure from native call. Expected {type: "data", data: ...}. Received: ' +
                    JSON.stringify(resultFromBlock)));
            }
        } catch (error) {
            console.error('Error inside promiseWrapper execution:', error);
            reject(error);
        }
    });
}

function parseNativeResponse(response) {
    if (typeof response === 'string' && response.trim() !== '') {
        try {
            return JSON.parse(response);
        } catch (e) {
            throw new Error(`Invalid JSON response from native: ${response}. Error: ${e.message}`);
        }
    } else if (response === undefined || response === null || (typeof response === 'string' && response.trim() === '')) {
        throw new Error('Empty or no response from native bridge.');
    } else {
        throw new Error(`Unexpected response type from native bridge: ${typeof response}`);
    }
}

// Send ready notification to Android bridge
(function notifyBridgeReady() {
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
    }
})();

console.log('Native Shipping API available at window.NativeShipping (for Android)');
