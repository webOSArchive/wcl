# Add project specific ProGuard rules here.

# Keep JavaScript interface methods
-keepclassmembers class com.example.wcl.bridge.AndroidBridge {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep Gson serialization classes
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
