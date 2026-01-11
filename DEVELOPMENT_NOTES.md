# WCL Development Notes

Technical learnings and solutions discovered during development.

## IPK File Handling

### IPK Structure
IPK files are AR archives containing:
- `debian-binary` - Version string
- `control.tar.gz` - Package metadata
- `data.tar.gz` - Application files (extract this)

### Extraction Process
1. Use `ArArchiveInputStream` to read the AR archive
2. Find and extract `data.tar.gz`
3. Use `TarArchiveInputStream` with `GzipCompressorInputStream` to extract contents
4. Handle `./` prefix in tar entries (webOS IPKs use `./` prefixed paths)
5. Look for `index.html` in the extracted directory tree

### Android Storage Considerations
- Android 11+ uses scoped storage - apps can't freely access `/sdcard/`
- Add `android:requestLegacyExternalStorage="true"` to AndroidManifest for Android 10 compatibility
- Request `READ_EXTERNAL_STORAGE` permission at runtime for older Android versions
- For Android 11+, consider using the Storage Access Framework or app-specific directories

## Framework Resource Loading

### The palmGetResource Problem
webOS apps use `palmGetResource(path, type)` to synchronously load framework resources, particularly g11n (globalization/locale) data. The path format is:
```
/usr/palm/frameworks/enyo/1.0/framework/g11n/...
```

### Solution
1. Add `loadResource()` method to `AndroidBridge.kt` that maps webOS paths to asset paths:
   - `/usr/palm/frameworks/enyo/0.10/framework/` → `frameworks/enyo/`
   - `/usr/palm/frameworks/enyo/1.0/framework/` → `frameworks/enyo/`
   - `/usr/palm/frameworks/mojo/` → `frameworks/mojo/`

2. Update `palm-system.js` to try `AndroidBridge.loadResource()` first for framework paths, falling back to XHR for other paths.

## CSS Compatibility Issues

### webkit-border-image Problem
webOS CSS uses `-webkit-border-image` which modern Android WebView doesn't support. This causes visual elements like calculator frames to disappear.

Example of webOS CSS:
```css
-webkit-border-image: url(../images/button-up.png) 4 4 4 4 stretch stretch;
```

### Solution: Two-Pronged Approach

**1. Modify bundled Enyo framework CSS files**

Add standard `border-image` alongside webkit prefix in all CSS files:
```bash
find . -name "*.css" -exec sed -i 's/-webkit-border-image:\([^;]*\);/-webkit-border-image:\1; border-image:\1;/g' {} \;
```

This handles apps that load Enyo from the WCL's bundled framework.

**2. Runtime JavaScript polyfill**

For apps that pack their own copy of Enyo, inject a polyfill that scans stylesheets:
```javascript
function fixWebkitBorderImages() {
    var sheets = document.styleSheets;
    for (var i = 0; i < sheets.length; i++) {
        try {
            var rules = sheets[i].cssRules || sheets[i].rules;
            if (!rules) continue;
            for (var j = 0; j < rules.length; j++) {
                var rule = rules[j];
                if (rule.style && rule.style.cssText) {
                    var cssText = rule.style.cssText;
                    if (cssText.indexOf('-webkit-border-image') !== -1 &&
                        cssText.indexOf('border-image:') === -1) {
                        var match = cssText.match(/-webkit-border-image:\s*([^;]+)/);
                        if (match) {
                            rule.style.borderImage = match[1];
                            if (!rule.style.borderStyle || rule.style.borderStyle === 'none') {
                                rule.style.borderStyle = 'solid';
                            }
                        }
                    }
                }
            }
        } catch (e) {} // Cross-origin stylesheets will throw
    }
}
```

**3. App-specific CSS injection**

For apps with CSS that isn't in the Enyo framework (like the calculator's own styles), inject CSS overrides:
```javascript
var basePath = window.location.href.replace(/\/[^\/]*$/, '/');
var style = document.createElement('style');
style.textContent = '.calc-small-body, .calc-tiny-body { ' +
    'border-style: solid !important; ' +
    'border-width: 40px !important; ' +
    'border-image-source: url(' + basePath + 'images/calc-bg.png) !important; ' +
    'border-image-slice: 40 40 40 40 fill !important; ' +
    'border-image-width: 40px !important; ' +
    'border-image-repeat: stretch !important; ' +
'}';
document.head.appendChild(style);
```

### Key border-image Requirements
- Must set `border-style: solid` (default is `none` which hides border-image)
- Must set `border-width` explicitly
- Use `fill` in `border-image-slice` to fill the center
- Use absolute URLs for images (relative URLs may resolve incorrectly)

## Viewport and Scaling

### TouchPad Resolution
The HP TouchPad has a 1024x768 screen. webOS apps were designed for this resolution.

### WebView Configuration
```kotlin
settings.loadWithOverviewMode = true
settings.useWideViewPort = true
```

### Viewport Meta Tag Injection
Inject a viewport tag to ensure proper scaling:
```javascript
var meta = document.createElement('meta');
meta.name = 'viewport';
meta.content = 'width=1024, user-scalable=yes';
document.head.appendChild(meta);
```

Wait for `document.head` to exist before injecting:
```javascript
function addViewport() {
    if (document.head && !document.querySelector('meta[name="viewport"]')) {
        // inject meta tag
    } else if (!document.head) {
        setTimeout(addViewport, 10);
    }
}
addViewport();
```

## Extracting Apps from TouchPad

### Using novacom
```bash
# List installed apps
novacom -t -d usb -- run file:///bin/ls /media/cryptofs/apps/usr/palm/applications/

# Extract an app
novacom -t -d usb -- run file:///bin/tar -czf - \
    /media/cryptofs/apps/usr/palm/applications/com.palm.app.calculator | \
    tar -xzf - -C ./extracted-apps/

# Create IPK from extracted app
cd extracted-apps/media/cryptofs/apps/usr/palm/applications/
ar -r com.palm.app.calculator_1.0.0_all.ipk \
    debian-binary control.tar.gz data.tar.gz
```

### App Locations on TouchPad
- System apps: `/usr/palm/applications/`
- User apps: `/media/cryptofs/apps/usr/palm/applications/`

## Testing Workflow

1. Make code changes
2. Build: `./gradlew assembleDebug`
3. Install: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
4. Test on device

## Future Considerations

### Other webkit CSS Properties
There may be other `-webkit-` prefixed CSS properties that need similar treatment:
- `-webkit-transform`
- `-webkit-transition`
- `-webkit-animation`
- `-webkit-box-shadow`
- `-webkit-gradient`

### Mojo Framework Support
Mojo apps (pre-Enyo) may have different requirements. The framework structure and APIs differ from Enyo.

### Service Completeness
Many luna services are stubbed. Real apps may need:
- `com.palm.db` (DB8) - Full database implementation
- `com.palm.audio` - Audio playback
- `com.palm.keys` - Hardware key handling
- `com.palm.preferences` - App preferences storage
