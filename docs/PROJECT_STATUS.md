# webOS Compatibility Layer - Project Status

## Overview

The webOS Compatibility Layer (WCL) enables running legacy HP webOS applications on Android devices. This project targets Enyo and Mojo web-based apps from the HP TouchPad era (webOS 3.0.5).

## Research Summary

### Resources Explored

| Resource | Location | Key Findings |
|----------|----------|--------------|
| HP TouchPad | Connected via novacom | webOS 3.0.5, Linux kernel 2.6.35, ARMv7 |
| PalmSDK | `/opt/PalmSDK` | Enyo 1.0, Mojo 506, palm-* tools, documentation |
| webOS OSS | `/home/jonwise/Projects/hp-webos-oss` | WebKit, Qt 4.8, GStreamer, SGX GPU drivers |
| webOS Doctor | `webosdoctorp305hstnhwifi.jar` | Full filesystem, 666 packages, LVM layout |
| Qt-webOS | `/home/jonwise/Projects/Qt-webOS` | Cross-compilation attempts, toolchain info |

### webOS App Architecture

webOS apps are web applications with special JavaScript APIs:

1. **PalmSystem** - Global object for system integration
   - Window management, keyboard control, notifications
   - Device info, locale, orientation
   - ~30 methods and properties

2. **PalmServiceBridge** - IPC for luna-service calls
   - URL format: `palm://service.name/method`
   - JSON request/response
   - Subscription support

3. **Frameworks**
   - **Enyo 1.0** - Component-based (TouchPad primary)
   - **Mojo** - Scene-based (Pre/Pixi legacy)

## Implementation

### Architecture

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

### Components Created

| Component | Files | Purpose |
|-----------|-------|---------|
| Android App | 5 Kotlin files | MainActivity, WebOSActivity, Bridge, Services |
| JS Shims | 2 JS files | PalmSystem, PalmServiceBridge APIs |
| Demo App | HTML/JS | Tests compatibility layer |
| Enyo Sampler | From PalmSDK samples | Real webOS Enyo app for testing |
| Enyo Framework | Bundled from SDK | Runtime for Enyo apps |

### File Structure

```
wcl/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/wcl/
│   │   │   ├── MainActivity.kt
│   │   │   ├── WebOSActivity.kt
│   │   │   ├── bridge/
│   │   │   │   └── AndroidBridge.kt
│   │   │   └── services/
│   │   │       ├── ServiceRouter.kt
│   │   │       ├── SystemService.kt
│   │   │       ├── ApplicationManagerService.kt
│   │   │       └── ConnectionManagerService.kt
│   │   └── assets/
│   │       ├── webos-shim/
│   │       │   ├── palm-system.js
│   │       │   └── palm-service-bridge.js
│   │       ├── frameworks/
│   │       │   └── enyo/
│   │       ├── demo-app/
│   │       └── webos-apps/
│   │           └── sampler/
│   └── build.gradle
├── docs/
│   ├── ARCHITECTURE.md
│   └── PROJECT_STATUS.md
└── README.md
```

### Supported APIs

#### PalmSystem Properties
- `activityId`, `identifier`, `launchParams`
- `locale`, `localeRegion`, `phoneRegion`
- `timeFormat`, `TZ`, `timezone`
- `deviceInfo`, `screenOrientation`
- `isActivated`

#### PalmSystem Methods
- `activate()`, `deactivate()`, `stageReady()`
- `setWindowProperties()`, `setWindowOrientation()`
- `keyboardShow()`, `keyboardHide()`
- `addBannerMessage()`, `removeBannerMessage()`
- `enableFullScreenMode()`

#### Luna Services Implemented
- `palm://com.palm.systemservice/*` - Time, locale, preferences
- `palm://com.palm.applicationManager/*` - App launching
- `palm://com.palm.connectionmanager/*` - Network status
- `palm://com.palm.db/*` - Database (basic stub)
- `palm://com.palm.power/*` - Battery status

## Building

```bash
cd /home/jonwise/Projects/wcl
./gradlew assembleDebug
# APK at app/build/outputs/apk/debug/app-debug.apk
```

## Testing

1. Install APK on Android device (API 26+)
2. Launch WCL
3. Select "Demo App" to test shim functionality
4. Select "Enyo Sampler" to test real webOS Enyo app

## Known Limitations

- PDK (native) apps not supported - only web-based Enyo/Mojo apps
- Some luna services return stub/mock responses
- Complex apps may require additional service implementations
- Hardware-specific features have limited support

## Future Enhancements

1. **More Services** - DB8, audio, camera, accelerometer
2. **IPK Extraction** - Built-in IPK file handling
3. **Mojo Framework** - Better scene stack support
4. **App Catalog** - Browse/install webOS apps
5. **Testing** - Validate more webOS applications

## Resources

- PalmSDK: `/opt/PalmSDK`
- webOS OSS: `/home/jonwise/Projects/hp-webos-oss`
- TouchPad access: `novacom -t -d usb -- run file:///bin/sh`
- webOS Archive: https://www.webosarchive.org/
