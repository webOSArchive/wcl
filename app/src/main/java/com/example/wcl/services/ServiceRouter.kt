package com.example.wcl.services

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * Routes luna-service calls to appropriate Android implementations
 */
class ServiceRouter(private val context: Context) {

    companion object {
        private const val TAG = "ServiceRouter"
    }

    private val gson = Gson()

    // Service implementations
    private val systemService = SystemService(context)
    private val applicationManagerService = ApplicationManagerService(context)
    private val connectionManagerService = ConnectionManagerService(context)

    /**
     * Route a service call to the appropriate handler
     * @param url The luna/palm URL (e.g., palm://com.palm.systemservice/time/getSystemTime)
     * @param paramsJson JSON-encoded parameters
     * @return JSON-encoded response
     */
    fun routeServiceCall(url: String, paramsJson: String): String {
        Log.d(TAG, "Routing: $url")

        // Parse URL: palm://service.name/category/method or luna://service.name/method
        val cleanUrl = url.removePrefix("palm://").removePrefix("luna://")
        val parts = cleanUrl.split("/")

        if (parts.isEmpty()) {
            return errorResponse("Invalid service URL: $url")
        }

        val serviceName = parts[0]
        val methodPath = parts.drop(1).joinToString("/")

        val params = try {
            if (paramsJson.isNotEmpty()) {
                JsonParser.parseString(paramsJson).asJsonObject
            } else {
                JsonObject()
            }
        } catch (e: Exception) {
            JsonObject()
        }

        return when (serviceName) {
            "com.palm.systemservice" -> systemService.handleCall(methodPath, params)
            "com.palm.applicationManager" -> applicationManagerService.handleCall(methodPath, params)
            "com.palm.connectionmanager" -> connectionManagerService.handleCall(methodPath, params)
            "com.palm.preferences" -> handlePreferencesService(methodPath, params)
            "com.palm.db" -> handleDBService(methodPath, params)
            "com.palm.audio" -> handleAudioService(methodPath, params)
            "com.palm.power" -> handlePowerService(methodPath, params)
            else -> {
                Log.w(TAG, "Unhandled service: $serviceName")
                stubResponse(serviceName, methodPath)
            }
        }
    }

    private fun handlePreferencesService(method: String, params: JsonObject): String {
        Log.d(TAG, "Preferences: $method")
        return when {
            method.contains("getPreferences") || method.contains("systemProperties") -> {
                JsonObject().apply {
                    addProperty("returnValue", true)
                    // Return empty preferences
                }.toString()
            }
            else -> stubResponse("com.palm.preferences", method)
        }
    }

    private fun handleDBService(method: String, params: JsonObject): String {
        Log.d(TAG, "DB: $method")
        // Basic DB stub - would need full implementation for real apps
        return when {
            method.contains("find") -> {
                JsonObject().apply {
                    addProperty("returnValue", true)
                    add("results", com.google.gson.JsonArray())
                }.toString()
            }
            method.contains("put") || method.contains("merge") -> {
                JsonObject().apply {
                    addProperty("returnValue", true)
                    add("results", com.google.gson.JsonArray().apply {
                        add(JsonObject().apply {
                            addProperty("id", "stub-id-${System.currentTimeMillis()}")
                            addProperty("rev", 1)
                        })
                    })
                }.toString()
            }
            method.contains("del") -> {
                JsonObject().apply {
                    addProperty("returnValue", true)
                    addProperty("count", 0)
                }.toString()
            }
            else -> stubResponse("com.palm.db", method)
        }
    }

    private fun handleAudioService(method: String, params: JsonObject): String {
        Log.d(TAG, "Audio: $method")
        return JsonObject().apply {
            addProperty("returnValue", true)
        }.toString()
    }

    private fun handlePowerService(method: String, params: JsonObject): String {
        Log.d(TAG, "Power: $method")
        return when {
            method.contains("batteryStatus") -> {
                JsonObject().apply {
                    addProperty("returnValue", true)
                    addProperty("percent", 100)
                    addProperty("charging", false)
                }.toString()
            }
            else -> JsonObject().apply {
                addProperty("returnValue", true)
            }.toString()
        }
    }

    private fun stubResponse(serviceName: String, method: String): String {
        Log.d(TAG, "Stub response for: $serviceName/$method")
        return JsonObject().apply {
            addProperty("returnValue", true)
            addProperty("_stubbed", true)
            addProperty("_service", serviceName)
            addProperty("_method", method)
        }.toString()
    }

    private fun errorResponse(message: String): String {
        return JsonObject().apply {
            addProperty("returnValue", false)
            addProperty("errorCode", -1)
            addProperty("errorText", message)
        }.toString()
    }
}
