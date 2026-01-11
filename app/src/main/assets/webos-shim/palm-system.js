/**
 * PalmSystem Shim for Android WebView
 *
 * This provides the window.PalmSystem object that webOS apps expect.
 * It bridges to native Android via the AndroidBridge interface.
 */
(function() {
    'use strict';

    // Don't reinitialize if already present
    if (window.PalmSystem && window.PalmSystem._wcl_initialized) {
        return;
    }

    console.log('[WCL] Initializing PalmSystem shim');

    // Generate a unique activity ID
    var activityId = 'wcl-' + Date.now() + '-' + Math.random().toString(36).substr(2, 9);

    // Get values from Android bridge
    var deviceInfo, locale, localeRegion, timeFormat, timezone;

    try {
        deviceInfo = AndroidBridge.getDeviceInfo();
        locale = AndroidBridge.getLocale();
        localeRegion = AndroidBridge.getLocaleRegion();
        timeFormat = AndroidBridge.getTimeFormat();
        timezone = AndroidBridge.getTimezone();
    } catch (e) {
        console.warn('[WCL] AndroidBridge not available, using defaults', e);
        deviceInfo = JSON.stringify({
            modelName: 'WCL Device',
            platformVersion: '3.0.5',
            screenWidth: window.innerWidth,
            screenHeight: window.innerHeight
        });
        locale = 'en_us';
        localeRegion = 'us';
        timeFormat = 'HH12';
        timezone = 'America/New_York';
    }

    // Window state
    var isActivated = true;
    var windowProperties = {};

    window.PalmSystem = {
        // Internal flag
        _wcl_initialized: true,

        // Properties (read-only in webOS)
        activityId: activityId,
        identifier: 'com.example.wcl.app',
        launchParams: '{}',
        locale: locale,
        localeRegion: localeRegion,
        phoneRegion: localeRegion,
        timeFormat: timeFormat,
        TZ: timezone,
        timezone: timezone,
        deviceInfo: deviceInfo,
        screenOrientation: 'up',
        specifiedWindowOrientation: '',
        videoOrientation: '',
        isMinimal: false,

        // Computed property
        get isActivated() {
            return isActivated;
        },

        // Window Management
        activate: function() {
            console.log('[WCL] PalmSystem.activate()');
            isActivated = true;
            window.dispatchEvent(new Event('activate'));
        },

        deactivate: function() {
            console.log('[WCL] PalmSystem.deactivate()');
            isActivated = false;
            window.dispatchEvent(new Event('deactivate'));
        },

        stageReady: function() {
            console.log('[WCL] PalmSystem.stageReady()');
            // Signal to system that the app stage is ready
        },

        setWindowProperties: function(props) {
            console.log('[WCL] PalmSystem.setWindowProperties()', props);
            if (typeof props === 'string') {
                try {
                    props = JSON.parse(props);
                } catch (e) {}
            }
            Object.assign(windowProperties, props);
            // Handle specific properties
            if (props.blockScreenTimeout !== undefined) {
                // Could use Android's wake lock
            }
            if (props.setSubtleLightbar !== undefined) {
                // Visual indicator - no-op on Android
            }
        },

        getWindowProperties: function() {
            return windowProperties;
        },

        setWindowOrientation: function(orientation) {
            console.log('[WCL] PalmSystem.setWindowOrientation()', orientation);
            this.specifiedWindowOrientation = orientation;
            // Could use Android's setRequestedOrientation
        },

        // Keyboard
        keyboardShow: function(type) {
            console.log('[WCL] PalmSystem.keyboardShow()', type);
            try {
                AndroidBridge.showKeyboard(type || 0);
            } catch (e) {
                console.warn('[WCL] Failed to show keyboard', e);
            }
        },

        keyboardHide: function() {
            console.log('[WCL] PalmSystem.keyboardHide()');
            try {
                AndroidBridge.hideKeyboard();
            } catch (e) {
                console.warn('[WCL] Failed to hide keyboard', e);
            }
        },

        setManualKeyboardEnabled: function(enabled) {
            console.log('[WCL] PalmSystem.setManualKeyboardEnabled()', enabled);
            // Store preference
        },

        // Clipboard
        paste: function() {
            console.log('[WCL] PalmSystem.paste()');
            // Trigger paste action - WebView handles this
            document.execCommand('paste');
        },

        // Notifications
        addBannerMessage: function(msg, params, iconPath, soundClass, soundFile, duration, doNotSuppress) {
            console.log('[WCL] PalmSystem.addBannerMessage()', msg);
            // Could show Android notification
            // For now, just log
            var id = 'banner-' + Date.now();
            return id;
        },

        removeBannerMessage: function(id) {
            console.log('[WCL] PalmSystem.removeBannerMessage()', id);
        },

        addNewContentIndicator: function() {
            console.log('[WCL] PalmSystem.addNewContentIndicator()');
            var id = 'content-' + Date.now();
            return id;
        },

        removeNewContentIndicator: function(id) {
            console.log('[WCL] PalmSystem.removeNewContentIndicator()', id);
        },

        // Display
        enableFullScreenMode: function(enable) {
            console.log('[WCL] PalmSystem.enableFullScreenMode()', enable);
            // Could toggle Android fullscreen mode
        },

        allowResizeOnPositiveSpaceChange: function(allow) {
            console.log('[WCL] PalmSystem.allowResizeOnPositiveSpaceChange()', allow);
        },

        // Input
        useSimulatedMouseClicks: function(use) {
            console.log('[WCL] PalmSystem.useSimulatedMouseClicks()', use);
        },

        simulateMouseClick: function(x, y, pressed) {
            console.log('[WCL] PalmSystem.simulateMouseClick()', x, y, pressed);
            // Create and dispatch mouse event
            var type = pressed ? 'mousedown' : 'mouseup';
            var event = new MouseEvent(type, {
                view: window,
                bubbles: true,
                cancelable: true,
                clientX: x,
                clientY: y
            });
            var element = document.elementFromPoint(x, y);
            if (element) {
                element.dispatchEvent(event);
            }
        },

        // Misc
        editorFocused: function(focused, x, y, width, height) {
            console.log('[WCL] PalmSystem.editorFocused()', focused);
        },

        printFrame: function(frameName, jobId, widthPx, heightPx, printDpi, landscape, reverseOrder) {
            console.log('[WCL] PalmSystem.printFrame()', frameName);
        },

        runTextIndexer: function(text, options) {
            console.log('[WCL] PalmSystem.runTextIndexer()');
            return text;
        },

        // Activity manager
        playSoundNotification: function(soundClass, soundFile, duration) {
            console.log('[WCL] PalmSystem.playSoundNotification()', soundClass, soundFile);
        },

        markFirstUseDone: function() {
            console.log('[WCL] PalmSystem.markFirstUseDone()');
        },

        // App menu
        prepareMenus: function() {
            // For Mojo framework
        }
    };

    // Add palmGetResource function used by some frameworks
    window.palmGetResource = function(path, type) {
        console.log('[WCL] palmGetResource()', path, type);

        // First try via AndroidBridge for framework paths
        if (path.indexOf('/usr/palm/frameworks/') !== -1) {
            try {
                var content = AndroidBridge.loadResource(path);
                if (content) {
                    if (type === 'const json' || type === 'json') {
                        try {
                            return JSON.parse(content);
                        } catch (e) {
                            console.warn('[WCL] palmGetResource failed to parse JSON:', path, e);
                            return null;
                        }
                    }
                    return content;
                }
            } catch (e) {
                console.log('[WCL] palmGetResource AndroidBridge fallback for:', path);
            }
        }

        // Fallback: Perform synchronous XHR to fetch the resource
        try {
            var xhr = new XMLHttpRequest();
            xhr.open('GET', path, false); // false = synchronous
            xhr.send(null);

            if (xhr.status === 200 || xhr.status === 0) { // 0 for file:// URLs
                var content = xhr.responseText;

                // If JSON type requested, parse it
                if (type === 'const json' || type === 'json') {
                    try {
                        return JSON.parse(content);
                    } catch (e) {
                        console.warn('[WCL] palmGetResource failed to parse JSON:', path, e);
                        return null;
                    }
                }

                return content;
            } else {
                console.warn('[WCL] palmGetResource failed:', path, xhr.status);
                return null;
            }
        } catch (e) {
            console.warn('[WCL] palmGetResource error:', path, e);
            return null;
        }
    };

    console.log('[WCL] PalmSystem shim initialized');
})();
