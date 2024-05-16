package com.settle.sample

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private var webView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.webView)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setupWebView()
        webView?.loadUrl("https://account.potlee.co.in/")
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun setupWebView() {
        webView = findViewById(R.id.webView)

        webView?.settings?.apply {
            // Enable DOM storage for saving user data.
            domStorageEnabled = true
            // Enable javascript
            javaScriptEnabled = true
            // Enable geolocation
            setGeolocationEnabled(true)
            // Allow media playback without user gesture
            mediaPlaybackRequiresUserGesture = false
        }

        webView?.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest?) {
                // Check if camera permission is granted
                if (ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    // Grant camera permission
                    request?.grant(request.resources)
                } else {
                    // Ask for camera permission and then grant the permission
                }
            }

            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissions.Callback?
            ) {
                // Check if location permissions are granted
                if (ContextCompat.checkSelfPermission(
                        this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    // Grant location permission
                    callback?.invoke(origin, true, false)
                } else {
                    // Ask for location permission and then grant the permission
                }
            }
        }

        webView?.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                // Handle URL redirection here
                return super.shouldOverrideUrlLoading(view, request)
            }
        }
    }
}