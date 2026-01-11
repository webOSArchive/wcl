package com.example.wcl

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewAssetLoader
import com.example.wcl.bridge.AndroidBridge
import com.example.wcl.databinding.ActivityWebosBinding
import java.io.*
import java.util.zip.GZIPInputStream

class WebOSActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_APP_PATH = "app_path"
        private const val TAG = "WebOSActivity"

        // Map webOS framework paths to our asset paths
        private val FRAMEWORK_PATH_MAP = mapOf(
            "/usr/palm/frameworks/enyo/0.10/framework/" to "frameworks/enyo/",
            "/usr/palm/frameworks/enyo/1.0/framework/" to "frameworks/enyo/",
            "/usr/palm/frameworks/mojo/" to "frameworks/mojo/"
        )
    }

    private lateinit var binding: ActivityWebosBinding
    private lateinit var androidBridge: AndroidBridge
    private var currentAppBasePath: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val appPath = intent.getStringExtra(EXTRA_APP_PATH)
            ?: "file:///android_asset/demo-app/index.html"

        setupWebView()
        loadWebOSApp(appPath)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        androidBridge = AndroidBridge(this, binding.webosWebView)

        binding.webosWebView.apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                allowFileAccessFromFileURLs = true
                allowUniversalAccessFromFileURLs = true
                mediaPlaybackRequiresUserGesture = false
                loadWithOverviewMode = true  // Scale to fit screen
                useWideViewPort = true
                builtInZoomControls = true
                displayZoomControls = false
                setSupportZoom(true)

                // Enable debugging
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    WebView.setWebContentsDebuggingEnabled(true)
                }
            }

            // Add the JavaScript interface for native communication
            addJavascriptInterface(androidBridge, "AndroidBridge")

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    binding.loadingProgress.visibility = View.VISIBLE

                    // Inject the PalmSystem shim before the page loads
                    injectPalmSystemShim(view)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    binding.loadingProgress.visibility = View.GONE
                    Log.d(TAG, "Page finished loading: $url")
                }

                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    // Handle luna:// and palm:// URLs
                    val url = request?.url?.toString() ?: return false
                    if (url.startsWith("luna://") || url.startsWith("palm://")) {
                        Log.d(TAG, "Intercepted service URL: $url")
                        return true
                    }
                    return false
                }

                override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                    val url = request?.url?.toString() ?: return null
                    val path = request.url.path ?: return null

                    Log.d(TAG, "Intercepting request - URL: $url, Path: $path")

                    // Check if this is a request for webOS frameworks
                    for ((webosPath, assetPath) in FRAMEWORK_PATH_MAP) {
                        if (path.startsWith(webosPath) || url.contains(webosPath)) {
                            val relativePath = if (path.startsWith(webosPath)) {
                                path.removePrefix(webosPath)
                            } else {
                                url.substringAfter(webosPath)
                            }
                            val assetFilePath = assetPath + relativePath
                            Log.d(TAG, "Redirecting $path to asset: $assetFilePath")

                            return try {
                                val inputStream = assets.open(assetFilePath)
                                val mimeType = getMimeType(assetFilePath)
                                WebResourceResponse(mimeType, "UTF-8", inputStream)
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to load asset: $assetFilePath", e)
                                null
                            }
                        }
                    }

                    // Check for relative paths from the app directory (for assets)
                    if (path.startsWith("/") && currentAppBasePath.isNotEmpty() && !currentAppBasePath.startsWith("/")) {
                        val assetPath = currentAppBasePath + path
                        return try {
                            val inputStream = assets.open(assetPath)
                            val mimeType = getMimeType(assetPath)
                            WebResourceResponse(mimeType, "UTF-8", inputStream)
                        } catch (e: Exception) {
                            // Asset not found, let WebView handle it
                            null
                        }
                    }

                    // For file:// URLs loading from extracted apps, serve framework from assets
                    if (url.startsWith("file:///usr/palm/frameworks/")) {
                        for ((webosPath, assetPath) in FRAMEWORK_PATH_MAP) {
                            if (url.contains(webosPath)) {
                                val relativePath = url.substringAfter(webosPath)
                                val assetFilePath = assetPath + relativePath
                                Log.d(TAG, "File URL framework redirect: $url to asset: $assetFilePath")

                                return try {
                                    val inputStream = assets.open(assetFilePath)
                                    val mimeType = getMimeType(assetFilePath)
                                    WebResourceResponse(mimeType, "UTF-8", inputStream)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to load asset: $assetFilePath", e)
                                    null
                                }
                            }
                        }
                    }

                    return null
                }

                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    Log.e(TAG, "WebView error: ${error?.description} for ${request?.url}")
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                    Log.d(TAG, "Console: ${consoleMessage?.message()} -- From line ${consoleMessage?.lineNumber()} of ${consoleMessage?.sourceId()}")
                    return true
                }

                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                }
            }
        }
    }

    private fun getMimeType(path: String): String {
        return when {
            path.endsWith(".html") -> "text/html"
            path.endsWith(".js") -> "application/javascript"
            path.endsWith(".css") -> "text/css"
            path.endsWith(".json") -> "application/json"
            path.endsWith(".png") -> "image/png"
            path.endsWith(".jpg") || path.endsWith(".jpeg") -> "image/jpeg"
            path.endsWith(".gif") -> "image/gif"
            path.endsWith(".svg") -> "image/svg+xml"
            path.endsWith(".woff") -> "font/woff"
            path.endsWith(".woff2") -> "font/woff2"
            path.endsWith(".ttf") -> "font/ttf"
            else -> "application/octet-stream"
        }
    }

    private fun injectPalmSystemShim(webView: WebView?) {
        try {
            // Inject viewport meta tag and CSS compatibility fixes
            val viewportScript = """
                (function() {
                    function addViewport() {
                        if (document.head && !document.querySelector('meta[name="viewport"]')) {
                            var meta = document.createElement('meta');
                            meta.name = 'viewport';
                            meta.content = 'width=1024, user-scalable=yes';
                            document.head.appendChild(meta);
                        } else if (!document.head) {
                            setTimeout(addViewport, 10);
                        }
                    }
                    addViewport();

                    // Generic webkit border-image polyfill for apps with packed-in frameworks
                    function fixWebkitBorderImages() {
                        if (!document.body) {
                            setTimeout(fixWebkitBorderImages, 100);
                            return;
                        }

                        // Process all stylesheets to find -webkit-border-image rules
                        var sheets = document.styleSheets;
                        for (var i = 0; i < sheets.length; i++) {
                            try {
                                var rules = sheets[i].cssRules || sheets[i].rules;
                                if (!rules) continue;

                                for (var j = 0; j < rules.length; j++) {
                                    var rule = rules[j];
                                    if (rule.style && rule.style.cssText) {
                                        var cssText = rule.style.cssText;
                                        // Check if has webkit-border-image but no standard border-image
                                        if (cssText.indexOf('-webkit-border-image') !== -1 &&
                                            cssText.indexOf('border-image:') === -1) {
                                            // Extract the webkit value and add standard property
                                            var match = cssText.match(/-webkit-border-image:\s*([^;]+)/);
                                            if (match) {
                                                rule.style.borderImage = match[1];
                                                // Also ensure border-style is set
                                                if (!rule.style.borderStyle || rule.style.borderStyle === 'none') {
                                                    rule.style.borderStyle = 'solid';
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (e) {
                                // Cross-origin stylesheets will throw - ignore
                            }
                        }
                        console.log('[WCL] Webkit border-image polyfill applied');
                    }

                    // Calculator-specific CSS fix (for apps with their own CSS)
                    function injectAppSpecificFixes() {
                        if (!document.head) {
                            setTimeout(injectAppSpecificFixes, 50);
                            return;
                        }
                        var basePath = window.location.href.replace(/\/[^\/]*$/, '/');
                        var style = document.createElement('style');
                        style.id = 'wcl-border-image-fix';
                        style.textContent = '' +
                            '.calc-small-body, .calc-tiny-body { ' +
                            '  border-style: solid !important; ' +
                            '  border-width: 40px !important; ' +
                            '  border-image-source: url(' + basePath + 'images/calc-bg.png) !important; ' +
                            '  border-image-slice: 40 40 40 40 fill !important; ' +
                            '  border-image-width: 40px !important; ' +
                            '  border-image-repeat: stretch !important; ' +
                            '} ' +
                            '.calc-body { ' +
                            '  border-style: solid !important; ' +
                            '  border-width: 24px !important; ' +
                            '  border-image-source: url(' + basePath + 'images/backdrop.png) !important; ' +
                            '  border-image-slice: 24 24 24 24 fill !important; ' +
                            '  border-image-width: 24px !important; ' +
                            '  border-image-repeat: repeat !important; ' +
                            '} ' +
                            '.calc-display { ' +
                            '  border-style: solid !important; ' +
                            '  border-width: 0 20px 0 20px !important; ' +
                            '  border-image-source: url(' + basePath + 'images/lcd-readout.png) !important; ' +
                            '  border-image-slice: 0 25 0 25 fill !important; ' +
                            '  border-image-width: 0 20px 0 20px !important; ' +
                            '  border-image-repeat: stretch !important; ' +
                            '}';
                        document.head.appendChild(style);
                    }
                    injectAppSpecificFixes();

                    // Run after page load to catch all stylesheets
                    if (document.readyState === 'complete') {
                        fixWebkitBorderImages();
                    } else {
                        window.addEventListener('load', fixWebkitBorderImages);
                    }
                })();
            """.trimIndent()

            val shimScript = loadAsset("webos-shim/palm-system.js")
            val bridgeScript = loadAsset("webos-shim/palm-service-bridge.js")

            webView?.evaluateJavascript(viewportScript, null)
            webView?.evaluateJavascript(shimScript, null)
            webView?.evaluateJavascript(bridgeScript, null)

            Log.d(TAG, "PalmSystem shim injected")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to inject PalmSystem shim", e)
        }
    }

    private fun loadAsset(fileName: String): String {
        return assets.open(fileName).bufferedReader().use { it.readText() }
    }

    private fun loadWebOSApp(appPath: String) {
        Log.d(TAG, "Loading webOS app from: $appPath")

        // Check if this is an IPK file
        if (appPath.endsWith(".ipk")) {
            val ipkFile = File(appPath.removePrefix("file://"))
            if (ipkFile.exists()) {
                val extractedPath = extractIpk(ipkFile)
                if (extractedPath != null) {
                    loadWebOSApp(extractedPath)
                    return
                } else {
                    Log.e(TAG, "Failed to extract IPK: $appPath")
                }
            } else {
                Log.e(TAG, "IPK file not found: $appPath")
            }
        }

        val url: String
        when {
            appPath.startsWith("file:///android_asset/") -> {
                // Extract base path for asset-based apps
                currentAppBasePath = appPath
                    .removePrefix("file:///android_asset/")
                    .substringBeforeLast("/")
                url = appPath
            }
            appPath.startsWith("file://") || appPath.startsWith("http") -> {
                url = appPath
            }
            appPath.startsWith("/") -> {
                url = "file://$appPath/index.html"
            }
            else -> {
                // Assume it's an asset path
                currentAppBasePath = appPath
                url = "file:///android_asset/$appPath/index.html"
            }
        }

        Log.d(TAG, "Current app base path: $currentAppBasePath")
        binding.webosWebView.loadUrl(url)
    }

    private fun extractIpk(ipkFile: File): String? {
        try {
            Log.d(TAG, "Extracting IPK: ${ipkFile.absolutePath}")

            // Create extraction directory in app cache
            val extractDir = File(cacheDir, "ipk-extract/${ipkFile.nameWithoutExtension}")
            if (extractDir.exists()) {
                extractDir.deleteRecursively()
            }
            extractDir.mkdirs()

            // IPK is an AR archive containing debian-binary, control.tar.gz, data.tar.gz
            val arInputStream = FileInputStream(ipkFile)

            // Skip AR header "!<arch>\n" (8 bytes)
            val arHeader = ByteArray(8)
            arInputStream.read(arHeader)
            val arHeaderStr = String(arHeader)
            if (!arHeaderStr.startsWith("!<arch>")) {
                Log.e(TAG, "Invalid AR archive header: $arHeaderStr")
                arInputStream.close()
                return null
            }

            // Read AR entries to find data.tar.gz
            var dataTarGz: ByteArray? = null
            while (arInputStream.available() > 0) {
                // AR entry header: 60 bytes
                // filename: 16 bytes, timestamp: 12, owner: 6, group: 6, mode: 8, size: 10, magic: 2
                val entryHeader = ByteArray(60)
                val bytesRead = arInputStream.read(entryHeader)
                if (bytesRead < 60) break

                val fileName = String(entryHeader, 0, 16).trim()
                val sizeStr = String(entryHeader, 48, 10).trim()
                val size = sizeStr.toIntOrNull() ?: 0

                Log.d(TAG, "AR entry: $fileName, size: $size")

                val content = ByteArray(size)
                arInputStream.read(content)

                // AR entries are padded to even byte boundaries
                if (size % 2 != 0) {
                    arInputStream.skip(1)
                }

                if (fileName.startsWith("data.tar")) {
                    dataTarGz = content
                }
            }
            arInputStream.close()

            if (dataTarGz == null) {
                Log.e(TAG, "data.tar.gz not found in IPK")
                return null
            }

            // Extract data.tar.gz
            val gzipStream = GZIPInputStream(ByteArrayInputStream(dataTarGz))
            extractTar(gzipStream, extractDir)
            gzipStream.close()

            // Find the app directory (should be in usr/palm/applications/xxx/)
            val appsDir = File(extractDir, "usr/palm/applications")
            if (appsDir.exists() && appsDir.isDirectory) {
                val appDirs = appsDir.listFiles()?.filter { it.isDirectory }
                if (!appDirs.isNullOrEmpty()) {
                    val appDir = appDirs[0]
                    Log.d(TAG, "Found app at: ${appDir.absolutePath}")
                    return appDir.absolutePath
                }
            }

            // Fallback: look for index.html directly
            val indexFile = findIndexHtml(extractDir)
            if (indexFile != null) {
                return indexFile.parentFile?.absolutePath
            }

            Log.e(TAG, "Could not find app in extracted IPK")
            return null

        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract IPK", e)
            return null
        }
    }

    private fun extractTar(inputStream: InputStream, destDir: File) {
        // Simple TAR extraction
        val buffer = ByteArray(512)

        while (true) {
            // Read TAR header (512 bytes)
            var bytesRead = 0
            while (bytesRead < 512) {
                val read = inputStream.read(buffer, bytesRead, 512 - bytesRead)
                if (read < 0) return
                bytesRead += read
            }

            // Check for end of archive (two zero blocks)
            if (buffer.all { it == 0.toByte() }) {
                break
            }

            // Parse header
            var fileName = String(buffer, 0, 100).trim('\u0000', ' ')
            if (fileName.isEmpty()) break
            // Remove leading ./ from paths
            if (fileName.startsWith("./")) {
                fileName = fileName.substring(2)
            }
            if (fileName.isEmpty()) continue

            val sizeOctal = String(buffer, 124, 12).trim('\u0000', ' ')
            val size = try {
                sizeOctal.toLong(8)
            } catch (e: NumberFormatException) {
                0L
            }
            val typeFlag = buffer[156]

            val destFile = File(destDir, fileName)

            when (typeFlag.toInt().toChar()) {
                '5', 'd' -> {
                    // Directory
                    destFile.mkdirs()
                }
                '0', '\u0000', ' ' -> {
                    // Regular file
                    destFile.parentFile?.mkdirs()
                    if (size > 0) {
                        val fileContent = ByteArray(size.toInt())
                        var totalRead = 0
                        while (totalRead < size) {
                            val read = inputStream.read(fileContent, totalRead, (size - totalRead).toInt())
                            if (read < 0) break
                            totalRead += read
                        }
                        FileOutputStream(destFile).use { it.write(fileContent) }

                        // Skip padding to 512-byte boundary
                        val padding = (512 - (size % 512)) % 512
                        inputStream.skip(padding)
                    }
                }
                else -> {
                    // Skip other types (links, etc.)
                    if (size > 0) {
                        val blocks = (size + 511) / 512
                        inputStream.skip(blocks * 512)
                    }
                }
            }
        }
    }

    private fun findIndexHtml(dir: File): File? {
        val indexFile = File(dir, "index.html")
        if (indexFile.exists()) return indexFile

        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                val found = findIndexHtml(file)
                if (found != null) return found
            }
        }
        return null
    }

    override fun onBackPressed() {
        if (binding.webosWebView.canGoBack()) {
            binding.webosWebView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        binding.webosWebView.destroy()
        super.onDestroy()
    }
}
