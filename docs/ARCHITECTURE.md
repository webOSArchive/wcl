# webOS Compatibility Layer (WCL) for Android

## Overview

The webOS Compatibility Layer enables running HP webOS applications on Android devices. It targets Enyo and Mojo web-based apps from the HP TouchPad era (webOS 3.0.5).

## Architecture

### webOS App Architecture

webOS apps are web applications that run in a WebKit-based browser with special JavaScript APIs:

1. **PalmSystem** - Global object providing system integration:
   - Window management (`activate`, `deactivate`, `setWindowProperties`)
   - Keyboard control (`keyboardShow`, `keyboardHide`)
   - System info (`deviceInfo`, `locale`, `screenOrientation`)
   - Notifications (`addBannerMessage`, `removeBannerMessage`)
   - Activity tracking (`activityId`)

2. **PalmServiceBridge** - IPC mechanism for luna-service calls:
   - Apps call `palm://service.name/method` URLs
   - Requests are JSON-encoded
   - Responses come via callbacks

3. **Frameworks** - JavaScript libraries:
   - **Enyo 1.0** - Component-based framework (HP TouchPad primary)
   - **Mojo** - Scene-based framework (older Pre/Pixi devices)

### Android Compatibility Layer

```
┌─────────────────────────────────────────────────────────────┐
│                    Android Application                       │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────┐    │
│  │                   Android WebView                    │    │
│  │  ┌─────────────────────────────────────────────┐    │    │
│  │  │              webOS App (HTML/JS/CSS)         │    │    │
│  │  │  ┌─────────────┐    ┌─────────────────────┐ │    │    │
│  │  │  │ Enyo/Mojo   │    │  App Code           │ │    │    │
│  │  │  │ Framework   │    │  (index.html, etc)  │ │    │    │
│  │  │  └──────┬──────┘    └──────────┬──────────┘ │    │    │
│  │  │         │                      │            │    │    │
│  │  │         ▼                      ▼            │    │    │
│  │  │  ┌─────────────────────────────────────┐   │    │    │
│  │  │  │     PalmSystem & PalmServiceBridge   │   │    │    │
│  │  │  │         (Injected JavaScript)        │   │    │    │
│  │  │  └──────────────────┬──────────────────┘   │    │    │
│  │  └─────────────────────┼──────────────────────┘    │    │
│  └────────────────────────┼───────────────────────────┘    │
│                           │ @JavascriptInterface           │
│                           ▼                                 │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              Native Service Bridge                   │   │
│  │  ┌─────────────┐  ┌────────────┐  ┌─────────────┐  │   │
│  │  │ SystemService│  │  DBService │  │ AppManager  │  │   │
│  │  │   Shim      │  │    Shim    │  │    Shim     │  │   │
│  │  └─────────────┘  └────────────┘  └─────────────┘  │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### Components

#### 1. WebOSActivity (Java/Kotlin)
Main Android Activity that:
- Creates and configures WebView
- Injects PalmSystem shim JavaScript
- Handles @JavascriptInterface calls
- Manages app lifecycle

#### 2. PalmSystem Shim (JavaScript)
Injected into WebView, provides:
```javascript
window.PalmSystem = {
    // Properties
    activityId: "<generated-id>",
    identifier: "<app-id>",
    launchParams: "{}",
    locale: "en_us",
    localeRegion: "us",
    deviceInfo: "{...}",
    screenOrientation: "up",
    timeFormat: "HH12",

    // Methods
    activate: function() {...},
    deactivate: function() {...},
    stageReady: function() {...},
    setWindowProperties: function(props) {...},
    keyboardShow: function(type) {...},
    keyboardHide: function() {...},
    addBannerMessage: function(...) {...},
    // etc.
};
```

#### 3. PalmServiceBridge Shim (JavaScript)
Handles luna-service calls:
```javascript
function PalmServiceBridge() {
    this.onservicecallback = null;
}
PalmServiceBridge.prototype.call = function(url, params) {
    // Route to native Android via @JavascriptInterface
    AndroidBridge.serviceCall(this._id, url, params);
};
PalmServiceBridge.prototype.cancel = function() {
    AndroidBridge.cancelCall(this._id);
};
```

#### 4. Service Shims (Java/Kotlin)
Native implementations for commonly-used services:

| webOS Service | Android Implementation |
|--------------|------------------------|
| com.palm.systemservice | Android Settings/System |
| com.palm.applicationManager | PackageManager/ActivityManager |
| com.palm.db | SQLite/Room database |
| com.palm.connectionmanager | ConnectivityManager |
| com.palm.power | PowerManager |
| com.palm.keys | SharedPreferences |

### App Loading Process

1. Extract IPK file (it's a Debian package) to get app contents
2. Parse `appinfo.json` for app metadata
3. Load WebView with `file:///` URL pointing to extracted app
4. Inject PalmSystem/PalmServiceBridge shims before page load
5. Load Enyo/Mojo frameworks (bundled or from app)
6. App's `index.html` loads and runs

### Key Challenges & Solutions

#### Challenge 1: Cross-Origin Restrictions
WebView's `file://` protocol has strict CORS limits.

**Solution**: Use WebViewAssetLoader or a local HTTP server to serve app files with proper CORS headers.

#### Challenge 2: Service Dependencies
Apps expect ~40+ luna services to be available.

**Solution**:
- Implement shims for critical services (db8, applicationManager, systemservice)
- Return mock/stub responses for non-critical services
- Log unhandled service calls for future implementation

#### Challenge 3: Framework Loading
Enyo/Mojo expect specific paths like `/usr/palm/frameworks/`.

**Solution**:
- Bundle frameworks in assets
- Rewrite paths during injection or use Service Worker

#### Challenge 4: Touch Events
webOS used custom touch event handling.

**Solution**: Modern Android WebView handles touch events well; may need minor adjustments for PalmSystem.simulateMouseClick.

### File Structure

```
wcl/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/wcl/
│   │   │   ├── WebOSActivity.kt
│   │   │   ├── WebOSWebView.kt
│   │   │   ├── bridge/
│   │   │   │   ├── AndroidBridge.kt
│   │   │   │   └── ServiceRouter.kt
│   │   │   └── services/
│   │   │       ├── SystemService.kt
│   │   │       ├── DBService.kt
│   │   │       └── AppManagerService.kt
│   │   ├── assets/
│   │   │   ├── webos-shim/
│   │   │   │   ├── palm-system.js
│   │   │   │   └── palm-service-bridge.js
│   │   │   └── frameworks/
│   │   │       ├── enyo/
│   │   │       └── mojo/
│   │   └── res/
│   └── build.gradle
├── docs/
│   └── ARCHITECTURE.md
└── build.gradle
```

### Development Phases

**Phase 1: Core Infrastructure**
- WebView setup with JavaScript injection
- PalmSystem basic properties
- PalmServiceBridge routing

**Phase 2: Essential Services**
- com.palm.systemservice (time, locale)
- com.palm.applicationManager (launch, open)
- Basic com.palm.db operations

**Phase 3: App Compatibility**
- Test with stock webOS apps
- Add service implementations as needed
- Framework path handling

**Phase 4: Polish**
- IPK extraction/installation UI
- App launcher
- Settings and preferences
