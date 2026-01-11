package com.example.wcl.bridge

import android.content.Context
import android.os.Build
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.example.wcl.services.ServiceRouter
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.util.*

/**
 * AndroidBridge provides the @JavascriptInterface methods that allow
 * webOS JavaScript code to communicate with native Android.
 */
class AndroidBridge(
    private val context: Context,
    private val webView: WebView
) {
    companion object {
        private const val TAG = "AndroidBridge"
    }

    private val gson = Gson()
    private val serviceRouter = ServiceRouter(context)
    private val pendingRequests = mutableMapOf<String, String>()

    /**
     * Called by PalmServiceBridge.call() in JavaScript
     * Routes service calls to appropriate handlers
     */
    @JavascriptInterface
    fun serviceCall(requestId: String, url: String, paramsJson: String) {
        Log.d(TAG, "Service call: $url with params: $paramsJson (requestId: $requestId)")

        pendingRequests[requestId] = url

        try {
            val response = serviceRouter.routeServiceCall(url, paramsJson)
            sendServiceResponse(requestId, response)
        } catch (e: Exception) {
            Log.e(TAG, "Service call failed", e)
            sendServiceError(requestId, e.message ?: "Unknown error")
        }
    }

    /**
     * Called by PalmServiceBridge.cancel() in JavaScript
     */
    @JavascriptInterface
    fun cancelServiceCall(requestId: String) {
        Log.d(TAG, "Cancel service call: $requestId")
        pendingRequests.remove(requestId)
    }

    /**
     * Get device info for PalmSystem.deviceInfo
     */
    @JavascriptInterface
    fun getDeviceInfo(): String {
        val deviceInfo = JsonObject().apply {
            addProperty("modelName", Build.MODEL)
            addProperty("modelNameAscii", Build.MODEL.replace(Regex("[^\\x00-\\x7F]"), ""))
            addProperty("platformVersion", "3.0.5")
            addProperty("platformVersionMajor", 3)
            addProperty("platformVersionMinor", 0)
            addProperty("platformVersionDot", 5)
            addProperty("carrierName", "")
            addProperty("serialNumber", Build.SERIAL)
            addProperty("screenWidth", 1024)
            addProperty("screenHeight", 768)
            addProperty("minimumCardWidth", 320)
            addProperty("minimumCardHeight", 480)
            addProperty("maximumCardWidth", 1024)
            addProperty("maximumCardHeight", 768)
            addProperty("keyboardType", "QWERTY")
            addProperty("wifiAvailable", true)
            addProperty("bluetoothAvailable", true)
            addProperty("carrierAvailable", false)
            addProperty("coreNaviButton", false)
            addProperty("dockModeEnabled", false)
        }
        return gson.toJson(deviceInfo)
    }

    /**
     * Get system locale
     */
    @JavascriptInterface
    fun getLocale(): String {
        val locale = Locale.getDefault()
        return "${locale.language}_${locale.country}".lowercase()
    }

    /**
     * Get locale region
     */
    @JavascriptInterface
    fun getLocaleRegion(): String {
        return Locale.getDefault().country.lowercase()
    }

    /**
     * Get time format preference
     */
    @JavascriptInterface
    fun getTimeFormat(): String {
        val is24Hour = android.text.format.DateFormat.is24HourFormat(context)
        return if (is24Hour) "HH24" else "HH12"
    }

    /**
     * Get timezone
     */
    @JavascriptInterface
    fun getTimezone(): String {
        return TimeZone.getDefault().id
    }

    /**
     * Log message from JavaScript
     */
    @JavascriptInterface
    fun logMessage(level: String, message: String) {
        when (level.lowercase()) {
            "error" -> Log.e(TAG, "JS: $message")
            "warn" -> Log.w(TAG, "JS: $message")
            "info" -> Log.i(TAG, "JS: $message")
            else -> Log.d(TAG, "JS: $message")
        }
    }

    /**
     * Show keyboard
     */
    @JavascriptInterface
    fun showKeyboard(type: Int) {
        Log.d(TAG, "Show keyboard type: $type")
        webView.post {
            webView.requestFocus()
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(webView, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }
    }

    /**
     * Hide keyboard
     */
    @JavascriptInterface
    fun hideKeyboard() {
        Log.d(TAG, "Hide keyboard")
        webView.post {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(webView.windowToken, 0)
        }
    }

    /**
     * Load a resource file from assets (for palmGetResource)
     * Maps webOS framework paths to bundled assets
     */
    @JavascriptInterface
    fun loadResource(path: String): String? {
        Log.d(TAG, "loadResource: $path")

        // Map webOS framework paths to our asset paths
        val assetPath = when {
            path.contains("/usr/palm/frameworks/enyo/0.10/framework/") ->
                path.replace("/usr/palm/frameworks/enyo/0.10/framework/", "frameworks/enyo/")
            path.contains("/usr/palm/frameworks/enyo/1.0/framework/") ->
                path.replace("/usr/palm/frameworks/enyo/1.0/framework/", "frameworks/enyo/")
            path.contains("/usr/palm/frameworks/mojo/") ->
                path.replace("/usr/palm/frameworks/mojo/", "frameworks/mojo/")
            else -> return null
        }

        return try {
            context.assets.open(assetPath).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.d(TAG, "Resource not found: $assetPath")
            null
        }
    }

    /**
     * Send response back to JavaScript
     */
    private fun sendServiceResponse(requestId: String, response: String) {
        webView.post {
            val escapedResponse = response.replace("\\", "\\\\").replace("'", "\\'")
            webView.evaluateJavascript(
                "window._palmServiceBridgeCallback('$requestId', '$escapedResponse');",
                null
            )
        }
    }

    /**
     * Send error back to JavaScript
     */
    private fun sendServiceError(requestId: String, errorMessage: String) {
        val errorResponse = JsonObject().apply {
            addProperty("returnValue", false)
            addProperty("errorCode", -1)
            addProperty("errorText", errorMessage)
        }
        sendServiceResponse(requestId, gson.toJson(errorResponse))
    }
}
