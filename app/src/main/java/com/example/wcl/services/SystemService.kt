package com.example.wcl.services

import android.content.Context
import android.util.Log
import com.google.gson.JsonObject
import java.util.*

/**
 * Implements com.palm.systemservice
 * Provides time, locale, and system property information
 */
class SystemService(private val context: Context) {

    companion object {
        private const val TAG = "SystemService"
    }

    fun handleCall(method: String, params: JsonObject): String {
        Log.d(TAG, "Handling: $method")

        return when {
            method.contains("time/getSystemTime") || method == "getSystemTime" -> getSystemTime()
            method.contains("time/getSystemNetworkTime") -> getSystemTime()
            method.contains("getPreferences") -> getPreferences(params)
            method.contains("getPreferenceValues") -> getPreferenceValues(params)
            method.contains("setPreferences") -> setPreferences(params)
            method.contains("timezone/getTimeZoneFromEasData") -> getTimezoneInfo()
            else -> stubResponse(method)
        }
    }

    private fun getSystemTime(): String {
        val now = System.currentTimeMillis()
        val tz = TimeZone.getDefault()

        return JsonObject().apply {
            addProperty("returnValue", true)
            addProperty("utc", now / 1000)
            addProperty("localtime", (now + tz.rawOffset) / 1000)
            addProperty("offset", tz.rawOffset / 60000) // in minutes
            addProperty("timezone", tz.id)
            addProperty("TZ", tz.id)
            addProperty("timeZoneFile", tz.id)
            addProperty("NITZValid", false)
            addProperty("NITZValidTime", false)
            addProperty("NITZValidZone", false)
        }.toString()
    }

    private fun getTimezoneInfo(): String {
        val tz = TimeZone.getDefault()
        return JsonObject().apply {
            addProperty("returnValue", true)
            addProperty("timezone", tz.id)
            addProperty("offset", tz.rawOffset / 60000)
        }.toString()
    }

    private fun getPreferences(params: JsonObject): String {
        val keys = params.getAsJsonArray("keys")
        val result = JsonObject().apply {
            addProperty("returnValue", true)
        }

        // Provide default values for common system preferences
        keys?.forEach { key ->
            val keyStr = key.asString
            when (keyStr) {
                "locale" -> result.addProperty(keyStr, Locale.getDefault().toString().replace("_", "-"))
                "region" -> result.addProperty(keyStr, Locale.getDefault().country.lowercase())
                "timeFormat" -> result.addProperty(keyStr, if (android.text.format.DateFormat.is24HourFormat(context)) "HH24" else "HH12")
                "timeZone" -> result.addProperty(keyStr, TimeZone.getDefault().id)
                "ringtone" -> result.addProperty(keyStr, "")
                "wallpaper" -> result.addProperty(keyStr, "")
                else -> result.addProperty(keyStr, "")
            }
        }

        return result.toString()
    }

    private fun getPreferenceValues(params: JsonObject): String {
        return getPreferences(params)
    }

    private fun setPreferences(params: JsonObject): String {
        // In a real implementation, we would persist these
        Log.d(TAG, "setPreferences called (stubbed)")
        return JsonObject().apply {
            addProperty("returnValue", true)
        }.toString()
    }

    private fun stubResponse(method: String): String {
        return JsonObject().apply {
            addProperty("returnValue", true)
            addProperty("_stubbed", true)
            addProperty("_method", method)
        }.toString()
    }
}
