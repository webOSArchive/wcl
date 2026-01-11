/**
 * PalmServiceBridge Shim for Android WebView
 *
 * This provides the PalmServiceBridge object that webOS apps use
 * to communicate with luna-service system services.
 */
(function() {
    'use strict';

    // Don't reinitialize if already present and initialized by us
    if (window.PalmServiceBridge && window.PalmServiceBridge._wcl_initialized) {
        return;
    }

    console.log('[WCL] Initializing PalmServiceBridge shim');

    // Store for pending callbacks
    var pendingCallbacks = {};
    var nextRequestId = 1;

    /**
     * Callback handler called from native Android
     */
    window._palmServiceBridgeCallback = function(requestId, responseJson) {
        console.log('[WCL] Service callback for request:', requestId);
        var request = pendingCallbacks[requestId];
        if (request && request.callback) {
            try {
                request.callback(responseJson);
            } catch (e) {
                console.error('[WCL] Service callback error:', e);
            }
        }
        // Don't delete if it's a subscription
        if (request && !request.isSubscription) {
            delete pendingCallbacks[requestId];
        }
    };

    /**
     * PalmServiceBridge constructor
     * Used for making luna-service calls
     */
    function PalmServiceBridge() {
        this._requestId = 'psb-' + (nextRequestId++);
        this.onservicecallback = null;
        this._cancelled = false;
    }

    PalmServiceBridge._wcl_initialized = true;

    /**
     * Make a service call
     * @param {string} url - The service URL (e.g., palm://com.palm.systemservice/time/getSystemTime)
     * @param {string} params - JSON-encoded parameters
     */
    PalmServiceBridge.prototype.call = function(url, params) {
        var self = this;

        if (this._cancelled) {
            console.warn('[WCL] Attempted to call cancelled PalmServiceBridge');
            return;
        }

        console.log('[WCL] PalmServiceBridge.call()', url, params);

        // Parse params to check for subscription
        var parsedParams = {};
        try {
            parsedParams = JSON.parse(params || '{}');
        } catch (e) {}

        var isSubscription = parsedParams.subscribe === true || parsedParams.watch === true;

        // Store callback info
        pendingCallbacks[this._requestId] = {
            callback: function(response) {
                if (self.onservicecallback && !self._cancelled) {
                    self.onservicecallback(response);
                }
            },
            isSubscription: isSubscription,
            url: url
        };

        // Call native Android bridge
        try {
            AndroidBridge.serviceCall(this._requestId, url, params || '{}');
        } catch (e) {
            console.error('[WCL] Failed to call AndroidBridge.serviceCall:', e);
            // Return error response
            var errorResponse = JSON.stringify({
                returnValue: false,
                errorCode: -1,
                errorText: 'Failed to call native bridge: ' + e.message
            });
            if (this.onservicecallback) {
                setTimeout(function() {
                    self.onservicecallback(errorResponse);
                }, 0);
            }
        }
    };

    /**
     * Cancel a pending service call
     */
    PalmServiceBridge.prototype.cancel = function() {
        console.log('[WCL] PalmServiceBridge.cancel()', this._requestId);
        this._cancelled = true;

        // Remove from pending
        delete pendingCallbacks[this._requestId];

        // Notify native side
        try {
            AndroidBridge.cancelServiceCall(this._requestId);
        } catch (e) {
            // Ignore errors
        }
    };

    // Also provide webOS.service for newer apps
    window.webOS = window.webOS || {};
    window.webOS.service = window.webOS.service || {};

    /**
     * webOS.service.request - Alternative API used by some apps
     */
    window.webOS.service.request = function(uri, options) {
        options = options || {};

        var bridge = new PalmServiceBridge();

        bridge.onservicecallback = function(response) {
            var parsed;
            try {
                parsed = typeof response === 'string' ? JSON.parse(response) : response;
            } catch (e) {
                parsed = { returnValue: false, errorText: 'Parse error' };
            }

            if (parsed.returnValue === false) {
                if (options.onFailure) {
                    options.onFailure(parsed);
                }
            } else {
                if (options.onSuccess) {
                    options.onSuccess(parsed);
                }
            }
            if (options.onComplete) {
                options.onComplete(parsed);
            }
        };

        var params = options.parameters || {};
        if (options.subscribe) {
            params.subscribe = true;
        }

        var url = uri;
        if (options.method) {
            url = uri + (uri.endsWith('/') ? '' : '/') + options.method;
        }

        bridge.call(url, JSON.stringify(params));

        return {
            cancel: function() {
                bridge.cancel();
            }
        };
    };

    // For Mojo framework compatibility
    window.Mojo = window.Mojo || {};
    window.Mojo.Service = window.Mojo.Service || {};
    window.Mojo.Service.Request = function(url, options, requestOptions) {
        var bridge = new PalmServiceBridge();
        var self = this;

        this.cancelled = false;

        var success = options.onSuccess || function() {};
        var failure = options.onFailure || function() {};
        var complete = options.onComplete || function() {};

        bridge.onservicecallback = function(response) {
            if (self.cancelled) return;

            var parsed;
            try {
                parsed = typeof response === 'string' ? JSON.parse(response) : response;
            } catch (e) {
                parsed = { returnValue: false, errorText: 'Parse error' };
            }

            if (parsed.errorCode || parsed.returnValue === false) {
                failure(parsed, self);
            } else {
                success(parsed, self);
            }
            complete(parsed, self);
        };

        var params = options.parameters || {};

        var fullUrl = url;
        if (options.method) {
            fullUrl = url + (url.endsWith('/') ? '' : '/') + options.method;
        }

        // Add activity ID if available
        if (window.PalmSystem && window.PalmSystem.activityId && !params.$activity) {
            params.$activity = { activityId: window.PalmSystem.activityId };
        }

        bridge.call(fullUrl, JSON.stringify(params));

        this.cancel = function() {
            self.cancelled = true;
            bridge.cancel();
        };
    };

    // Export
    window.PalmServiceBridge = PalmServiceBridge;

    console.log('[WCL] PalmServiceBridge shim initialized');
})();
