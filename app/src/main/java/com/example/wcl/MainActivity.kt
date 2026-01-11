package com.example.wcl

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.wcl.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkStoragePermission()
        setupButtons()
    }

    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Storage permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Storage permission needed to load external IPK files", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupButtons() {
        binding.btnLoadDemo.setOnClickListener {
            // Load the built-in demo app
            launchWebOSApp("file:///android_asset/demo-app/index.html")
        }

        binding.btnLoadFromPath.setOnClickListener {
            showAppSelectionDialog()
        }
    }

    private fun showAppSelectionDialog() {
        val apps = arrayOf(
            "Demo App" to "demo-app",
            "Calculator (webOS)" to "webos-apps/calculator",
            "Enyo Sampler (webOS)" to "webos-apps/sampler",
            "Enter Custom Path..." to "custom"
        )

        AlertDialog.Builder(this)
            .setTitle("Select webOS App")
            .setItems(apps.map { it.first }.toTypedArray()) { _, which ->
                when (apps[which].second) {
                    "custom" -> showPathInputDialog()
                    else -> launchWebOSApp("file:///android_asset/${apps[which].second}/index.html")
                }
            }
            .show()
    }

    private fun showPathInputDialog() {
        val input = android.widget.EditText(this).apply {
            hint = "Enter path to webOS app (e.g., /sdcard/webos-apps/myapp)"
        }

        AlertDialog.Builder(this)
            .setTitle("Load webOS App")
            .setMessage("Enter the path to the webOS app directory or IPK file:")
            .setView(input)
            .setPositiveButton("Load") { _, _ ->
                val path = input.text.toString().trim()
                if (path.isNotEmpty()) {
                    launchWebOSApp(path)
                } else {
                    Toast.makeText(this, "Please enter a valid path", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun launchWebOSApp(appPath: String) {
        binding.statusText.text = "Status: Loading $appPath"

        val intent = Intent(this, WebOSActivity::class.java).apply {
            putExtra(WebOSActivity.EXTRA_APP_PATH, appPath)
        }
        startActivity(intent)
    }
}
