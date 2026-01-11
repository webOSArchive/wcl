package com.example.wcl.services

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.google.gson.JsonObject

/**
 * Implements com.palm.connectionmanager
 * Provides network connectivity information
 */
class ConnectionManagerService(private val context: Context) {

    companion object {
        private const val TAG = "ConnectionManagerService"
    }

    fun handleCall(method: String, params: JsonObject): String {
        Log.d(TAG, "Handling: $method")

        return when {
            method.contains("getStatus") || method == "getstatus" -> getStatus()
            method.contains("getinfo") -> getInfo()
            else -> stubResponse(method)
        }
    }

    private fun getStatus(): String {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = network?.let { connectivityManager.getNetworkCapabilities(it) }

        val isConnected = capabilities != null
        val isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val isCellular = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true

        return JsonObject().apply {
            addProperty("returnValue", true)
            addProperty("isInternetConnectionAvailable", isConnected)

            add("wifi", JsonObject().apply {
                addProperty("state", if (isWifi) "connected" else "disconnected")
                addProperty("ipAddress", "")
                addProperty("ssid", "")
                addProperty("bssid", "")
                addProperty("networkConfidenceLevel", "")
            })

            add("wan", JsonObject().apply {
                addProperty("state", if (isCellular) "connected" else "disconnected")
                addProperty("ipAddress", "")
                addProperty("network", "")
            })

            add("wired", JsonObject().apply {
                addProperty("state", "disconnected")
            })

            add("bridge", JsonObject().apply {
                addProperty("state", "disconnected")
            })
        }.toString()
    }

    private fun getInfo(): String {
        return getStatus()
    }

    private fun stubResponse(method: String): String {
        return JsonObject().apply {
            addProperty("returnValue", true)
            addProperty("_stubbed", true)
            addProperty("_method", method)
        }.toString()
    }
}
