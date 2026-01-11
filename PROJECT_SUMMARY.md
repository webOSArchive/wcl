Project Summary: webOS Compatibility Layer (WCL)

Goal: Run legacy HP webOS (TouchPad/Pre/Pixi) applications on modern Android devices.

Architecture: Android WebView hosts webOS web apps, with JavaScript shims implementing webOS-specific APIs (PalmSystem, PalmServiceBridge) that bridge to native Android via @JavascriptInterface.

What Works

- PalmSystem API - Device info, locale, timezone, keyboard, window management
- PalmServiceBridge - Luna service call routing to native implementations
- Enyo 1.0 framework - Bundled and functional
- palmGetResource() - Loads g11n/locale data from bundled frameworks
- IPK extraction - Load apps directly from IPK files
- CSS compatibility - webkit-border-image polyfill for proper visual rendering
- Calculator app - Fully functional with correct visual appearance

Key Technical Solutions

1. IPK files - AR archives containing data.tar.gz; handle ./ prefix in paths
2. palmGetResource - Bridge to AndroidBridge.loadResource() for framework paths
3. webkit-border-image - Two-pronged fix: modify Enyo CSS + runtime JS polyfill + app-specific overrides
4. Viewport scaling - Inject width=1024 meta tag for TouchPad resolution

Project Files

- WebOSActivity.kt - WebView setup, IPK extraction, JS injection
- AndroidBridge.kt - Native bridge for JS calls
- palm-system.js / palm-service-bridge.js - webOS API shims
- frameworks/enyo/ - Bundled Enyo framework (CSS modified for compatibility)

---
Prompt for Future Session

Continue development on the webOS Compatibility Layer (WCL) project at /home/jonwise/Projects/wcl

WCL is an Android app that runs legacy HP webOS applications using WebView with JavaScript shims for PalmSystem and PalmServiceBridge APIs.

Current status:
- Calculator app works with proper visual appearance
- IPK file loading works
- Enyo 1.0 framework bundled and functional
- webkit-border-image CSS compatibility implemented

Read DEVELOPMENT_NOTES.md for technical details on solutions we've implemented.

Next areas to explore:
1. Test more webOS apps and fix compatibility issues as they arise
2. Implement missing luna services (com.palm.db, com.palm.audio, etc.)
3. Add Mojo framework support for older apps
4. Improve IPK handling (app info dialog, icon extraction)
5. Add app launcher/manager for installed webOS apps

I have an HP TouchPad connected via novacom for extracting apps to test.
