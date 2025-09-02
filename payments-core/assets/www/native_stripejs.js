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
                        stackTrace: undefined
                    }));
                }
            } catch(e) {}
        };
    });
})();

// Initialize bridge communication for StripeJS Next Action
window.NativeStripeNextAction = {
    getInitParams: function() {
        if (window.androidBridge && typeof window.androidBridge.getInitParams === 'function') {
            const result = window.androidBridge.getInitParams();
            try {
                const parsed = JSON.parse(result);
                return parsed;
            } catch (e) {
                return result;
            }
        } else {
            throw new Error('Native bridge not available for method: getInitParams');
        }
    },

    onReady: function() {
        if (window.androidBridge && typeof window.androidBridge.onReady === 'function') {
            window.androidBridge.onReady();
        }
    },

    onSuccess: function(paymentIntent) {
        if (window.androidBridge && typeof window.androidBridge.onSuccess === 'function') {
            const paymentIntentJson = typeof paymentIntent === 'object' ?
                JSON.stringify(paymentIntent) : String(paymentIntent);
            window.androidBridge.onSuccess(paymentIntentJson);
        }
    },

    onError: function(errorMessage) {
        if (window.androidBridge && typeof window.androidBridge.onError === 'function') {
            window.androidBridge.onError(String(errorMessage));
        }
    }
};

// Notify native that the bridge is ready
if (window.androidBridge && typeof window.androidBridge.ready === 'function') {
    window.androidBridge.ready(JSON.stringify({
        type: 'bridgeReady',
        timestamp: Date.now()
    }));
}