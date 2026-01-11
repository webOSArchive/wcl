package com.example.wcl.services

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.google.gson.JsonObject

/**
 * Implements com.palm.applicationManager
 * Handles app launching and management
 */
class ApplicationManagerService(private val context: Context) {

    companion object {
        private const val TAG = "AppManagerService"
    }

    fun handleCall(method: String, params: JsonObject): String {
        Log.d(TAG, "Handling: $method with params: $params")

        return when {
            method == "launch" || method.endsWith("/launch") -> launchApp(params)
            method == "open" || method.endsWith("/open") -> openResource(params)
            method == "listApps" || method.endsWith("/listApps") -> listApps()
            method == "getAppInfo" || method.endsWith("/getAppInfo") -> getAppInfo(params)
            method == "running" || method.endsWith("/running") -> getRunningApps()
            else -> stubResponse(method)
        }
    }

    private fun launchApp(params: JsonObject): String {
        val appId = params.get("id")?.asString
        val launchParams = params.get("params")

        Log.d(TAG, "Launch request for app: $appId")

        // For webOS apps, we'd load them in our WebView
        // For now, return success
        return JsonObject().apply {
            addProperty("returnValue", true)
            addProperty("processId", "wcl-${System.currentTimeMillis()}")
        }.toString()
    }

    private fun openResource(params: JsonObject): String {
        val target = params.get("target")?.asString

        if (target != null) {
            Log.d(TAG, "Open resource: $target")

            try {
                when {
                    target.startsWith("http://") || target.startsWith("https://") -> {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(target))
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }
                    target.startsWith("tel:") -> {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse(target))
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }
                    target.startsWith("mailto:") -> {
                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse(target))
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }
                }
                return JsonObject().apply {
                    addProperty("returnValue", true)
                }.toString()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open resource", e)
                return JsonObject().apply {
                    addProperty("returnValue", false)
                    addProperty("errorText", e.message)
                }.toString()
            }
        }

        return JsonObject().apply {
            addProperty("returnValue", true)
        }.toString()
    }

    private fun listApps(): String {
        // Return empty list for now - could scan for installed webOS apps
        return JsonObject().apply {
            addProperty("returnValue", true)
            add("apps", com.google.gson.JsonArray())
        }.toString()
    }

    private fun getAppInfo(params: JsonObject): String {
        val appId = params.get("id")?.asString
        return JsonObject().apply {
            addProperty("returnValue", true)
            add("appInfo", JsonObject().apply {
                addProperty("id", appId ?: "")
                addProperty("title", "Unknown App")
                addProperty("version", "1.0.0")
            })
        }.toString()
    }

    private fun getRunningApps(): String {
        return JsonObject().apply {
            addProperty("returnValue", true)
            add("running", com.google.gson.JsonArray())
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
