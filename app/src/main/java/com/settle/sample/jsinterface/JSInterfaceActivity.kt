package com.settle.sample.jsinterface

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.webkit.GeolocationPermissions
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.commit
import com.settle.sample.BuildConfig
import com.settle.sample.R
import com.settle.sample.permissions.PermissionHandlerFragment
import com.settle.sample.permissions.PermissionType

class JSInterfaceActivity : AppCompatActivity() {

    private var webView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.web_view)
        webView = findViewById(R.id.web_view)

        setupWebView()
        setupFragmentResultListener()
    }

    private fun setupFragmentResultListener() {
        supportFragmentManager
            .setFragmentResultListener(
                PermissionHandlerFragment.RESULT_PERMISSION_KEY,
                this
            ) { _, bundle ->
                val permissionType: PermissionType? =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        bundle.getSerializable(
                            PermissionHandlerFragment.RESULT_PERMISSION_TYPE_KEY,
                            PermissionType::class.java
                        )
                    } else {
                        bundle.getSerializable(
                            PermissionHandlerFragment.RESULT_PERMISSION_TYPE_KEY
                        ) as? PermissionType
                    }

                val permissionGranted =
                    bundle.getBoolean(PermissionHandlerFragment.RESULT_PERMISSION_GRANTED_KEY)

                when (permissionType) {
                    PermissionType.Camera,
                    PermissionType.Location -> {
                        processJSPermission(
                            permissionType = permissionType,
                            permissionGranted = permissionGranted
                        )
                    }
                    null -> {
                        // Do nothing
                    }
                }
            }
    }

    private fun processJSPermission(
        permissionType: PermissionType,
        permissionGranted: Boolean,
    ) {
        when (permissionType) {
            PermissionType.Camera,
            PermissionType.Location -> {
                invokeJavascriptPermission(
                    permissionType = permissionType,
                    permissionGranted = permissionGranted
                )
            }
        }
    }

    private fun invokeJavascriptPermission(
        permissionType: PermissionType,
        permissionGranted: Boolean,
    ) {
        runOnUiThread {
            val permissionName = permissionType.name
            val javascriptCode =
                "window.processPermission(${permissionGranted}, '${permissionName}');"
            webView?.evaluateJavascript(javascriptCode, null)
        }
    }

    private fun setupWebView() {
        applyWebViewSettings()
        addJSInterface()
        addChromeClient()
        addWebViewClient()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun applyWebViewSettings() {
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
        webView?.loadUrl(BuildConfig.WEB_URL)
    }

    private fun addJSInterface() {
        webView?.addJavascriptInterface(
            object : Any() {
                // Used to grant camera permission to the webview
                @JavascriptInterface
                fun getLocationPermission(params: String) {
                    if (ContextCompat.checkSelfPermission(
                            this@JSInterfaceActivity,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(
                            this@JSInterfaceActivity,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        // Grant location permission
                        processJSPermission(
                            permissionType = PermissionType.Location,
                            permissionGranted = true
                        )
                    } else {
                        // Ask for location permission or redirect the user to application settings
                        // to grant the required permissions
                        askLocationPermission()
                    }
                }

                // Used to grant location permission to the webview
                @JavascriptInterface
                fun getCameraPermission(params: String) {
                    if (ContextCompat.checkSelfPermission(
                            this@JSInterfaceActivity,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        // Grant camera permission
                        processJSPermission(
                            permissionType = PermissionType.Camera,
                            permissionGranted = true
                        )
                    } else {
                        // Ask for camera permission or redirect the user to application settings
                        // to grant the required permissions
                        askCameraPermission()
                    }
                }
            }, "__settleAndroidKit"
        )
    }

    private fun addChromeClient() {
        webView?.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest?) {
                // Check if camera permission is granted
                if (ContextCompat.checkSelfPermission(
                        this@JSInterfaceActivity,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    // Grant camera permission
                    request?.grant(request.resources)
                } else {
                    // Ask for camera permission or redirect the user to application settings
                    // to grant the required permissions
                    askCameraPermission()
                }
            }

            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissions.Callback?
            ) {
                // Check if location permissions are granted
                if (ContextCompat.checkSelfPermission(
                        this@JSInterfaceActivity,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        this@JSInterfaceActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    // Grant location permission
                    callback?.invoke(origin, true, false)
                } else {
                    // Ask for location permission or redirect the user to application settings
                    // to grant the required permissions
                    askLocationPermission()
                }
            }
        }
    }

    private fun addWebViewClient() {
        webView?.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?,
            ): Boolean {
                // Handle URL redirection here
                return super.shouldOverrideUrlLoading(view, request)
            }
        }
    }

    private fun askCameraPermission() {
        addFragment(fragment = PermissionHandlerFragment.cameraPermissionInstance())
    }

    private fun askLocationPermission() {
        addFragment(fragment = PermissionHandlerFragment.locationPermissionInstance())
    }

    private fun addFragment(fragment: PermissionHandlerFragment) {
        supportFragmentManager.commit {
            add(
                fragment,
                PermissionHandlerFragment::class.simpleName
            )
        }
    }

    companion object {
        fun newInstance(context: Context): Intent {
            return Intent(context, JSInterfaceActivity::class.java)
        }
    }
}