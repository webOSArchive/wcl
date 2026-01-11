# WCL - webOS Compatibility Layer

Android app that runs legacy HP webOS (TouchPad/Pre/Pixi) applications.

## Build & Test

```bash
./gradlew assembleDebug                                    # Build
adb install -r app/build/outputs/apk/debug/app-debug.apk  # Install
adb logcat -s AndroidBridge WCL WebOSActivity             # Debug logs
```

## Architecture

WebView hosts webOS apps with JS shims bridging to native Android:

```
webOS App (HTML/JS)
    ↓
palm-system.js / palm-service-bridge.js (JS shims)
    ↓ @JavascriptInterface
AndroidBridge.kt → ServiceRouter.kt → *Service.kt
```

## Key Files

| File | Purpose |
|------|---------|
| `app/src/main/java/com/example/wcl/WebOSActivity.kt` | WebView setup, IPK extraction, JS/CSS injection |
| `app/src/main/java/com/example/wcl/bridge/AndroidBridge.kt` | Native bridge for JS calls |
| `app/src/main/java/com/example/wcl/services/ServiceRouter.kt` | Routes luna service calls |
| `app/src/main/assets/webos-shim/palm-system.js` | PalmSystem API implementation |
| `app/src/main/assets/webos-shim/palm-service-bridge.js` | PalmServiceBridge API implementation |
| `app/src/main/assets/frameworks/enyo/` | Bundled Enyo 1.0 framework |

## Technical Notes

### IPK Files
- AR archives containing `data.tar.gz` with app files
- Entries have `./` prefix in paths
- Extract to cache dir, find `index.html`

### palmGetResource()
- webOS apps load framework resources via `palmGetResource(path, type)`
- Paths like `/usr/palm/frameworks/enyo/1.0/framework/g11n/...`
- AndroidBridge.loadResource() maps these to bundled assets

### CSS Compatibility
- `-webkit-border-image` doesn't work in modern WebView
- Fixed via: modified Enyo CSS + runtime JS polyfill + app-specific overrides
- Must set `border-style: solid` and explicit `border-width`
- Use absolute URLs for border-image sources

### Viewport
- TouchPad resolution: 1024x768
- Inject `<meta name="viewport" content="width=1024, user-scalable=yes">`

## Extracting Apps from TouchPad

```bash
# List apps
novacom -t -d usb -- run file:///bin/ls /media/cryptofs/apps/usr/palm/applications/

# Extract app
novacom -t -d usb -- run file:///bin/tar -czf - \
    /media/cryptofs/apps/usr/palm/applications/com.palm.app.calculator | \
    tar -xzf - -C ./extracted-apps/
```

## What Works
- PalmSystem API (device info, locale, keyboard, window management)
- PalmServiceBridge (luna service routing)
- Enyo 1.0 framework
- IPK file loading
- Calculator app (fully functional)

## Known Gaps
- Mojo framework (older apps)
- Many luna services stubbed (com.palm.db, com.palm.audio)
- PDK/native apps not supported
- Hardware features (accelerometer, camera) limited

## Code Style
- Kotlin for Android code
- 4-space indentation
- Descriptive variable names
- Log with TAG constants: `Log.d(TAG, "message")`
