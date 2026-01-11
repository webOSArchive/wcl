# webOS Compatibility Layer (WCL) for Android

Run legacy HP webOS applications on Android devices.

## Overview

WCL is an Android application that provides a compatibility layer for running webOS applications (originally designed for the HP TouchPad and Palm Pre/Pixi devices) on modern Android devices.

## Features

- **PalmSystem API** - Full JavaScript implementation of the webOS PalmSystem object
- **PalmServiceBridge** - Luna service call routing to native Android implementations
- **Framework Support** - Bundled Enyo 1.0 framework (Mojo support planned)
- **Service Shims** - Android implementations of common webOS services:
  - `com.palm.systemservice` - Time, locale, preferences
  - `com.palm.applicationManager` - App launching and management
  - `com.palm.connectionmanager` - Network connectivity
  - `com.palm.db` - Database operations (basic)
  - `com.palm.power` - Battery status

## Requirements

- Android 8.0 (API 26) or higher
- ARM-based Android device (recommended for best compatibility)

## Building

1. Clone the repository
2. Open in Android Studio
3. Build and run on your device

```bash
./gradlew assembleDebug
```

## Usage

### Running the Demo App

1. Launch WCL on your Android device
2. Tap "Load Demo App" to test the compatibility layer
3. Or select "Enyo Sampler" to run a real webOS Enyo application

### Running webOS Apps

1. Extract a webOS app from your TouchPad or IPK file
2. Place the app in `/sdcard/webos-apps/` or your preferred location
3. In WCL, tap "Load from Path" and select your app

### Loading Apps from TouchPad

If you have a TouchPad connected via novacom:

```bash
# Extract an app
novacom -t -d usb -- run file:///bin/tar -czf - /usr/palm/applications/com.palm.app.help | tar -xzf -

# Or copy directly
novacom -t -d usb get file:///usr/palm/applications/com.palm.app.browser /path/to/local/browser
```

## Architecture

WCL uses Android WebView to run webOS web applications, with JavaScript shims that implement the webOS-specific APIs:

```
┌─────────────────────────────────────────┐
│          Android Application            │
├─────────────────────────────────────────┤
│  ┌─────────────────────────────────┐   │
│  │         Android WebView          │   │
│  │  ┌───────────────────────────┐  │   │
│  │  │     webOS App (HTML/JS)   │  │   │
│  │  │  + PalmSystem shim        │  │   │
│  │  │  + PalmServiceBridge shim │  │   │
│  │  └─────────────┬─────────────┘  │   │
│  └────────────────┼────────────────┘   │
│                   │ @JavascriptInterface│
│                   ▼                     │
│  ┌─────────────────────────────────┐   │
│  │      Native Service Bridge       │   │
│  │  (SystemService, AppManager,     │   │
│  │   ConnectionManager, etc.)       │   │
│  └─────────────────────────────────┘   │
└─────────────────────────────────────────┘
```

## Supported webOS APIs

### PalmSystem Properties
- `activityId`, `identifier`, `launchParams`
- `locale`, `localeRegion`, `phoneRegion`
- `timeFormat`, `TZ`, `timezone`
- `deviceInfo`, `screenOrientation`
- `isActivated`

### PalmSystem Methods
- `activate()`, `deactivate()`, `stageReady()`
- `setWindowProperties()`, `setWindowOrientation()`
- `keyboardShow()`, `keyboardHide()`
- `addBannerMessage()`, `removeBannerMessage()`
- `enableFullScreenMode()`
- And more...

### Luna Services
- `palm://com.palm.systemservice/*`
- `palm://com.palm.applicationManager/*`
- `palm://com.palm.connectionmanager/*`
- `palm://com.palm.db/*` (basic)
- `palm://com.palm.power/*`

## Project Structure

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
│   └── build.gradle
├── docs/
│   └── ARCHITECTURE.md
└── README.md
```

## Known Limitations

- PDK (native) apps are not supported - only web-based Enyo/Mojo apps
- Some luna services are stubbed with mock responses
- Complex apps may require additional service implementations
- TouchPad-specific hardware features (accelerometer, camera) have limited support

## Contributing

Contributions are welcome! Areas that need work:

1. **More Service Implementations** - Many luna services are stubbed
2. **Mojo Framework** - Better support for older Mojo-based apps
3. **DB8 Service** - Full database implementation
4. **IPK Extraction** - Built-in IPK file handling
5. **Testing** - More webOS apps need testing

## Resources

- [PalmSDK Documentation](https://sdk.webosarchive.org/)
- [webOS Archive](https://www.webosarchive.org/)
- [Enyo Framework](https://github.com/nicko88/nicko88.github.io/tree/master/mojo-the-game/lib/enyo)

## License

MIT License - See LICENSE file for details.

## Acknowledgments

- HP/Palm for creating webOS
- The webOS community for preserving these apps
- Contributors to the webOS Archive project
