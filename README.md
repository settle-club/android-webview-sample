# Kotlin

### Android Webview Integration steps

To ensure a safe and personalized experience, and to comply with regulations that prevent fraud, we require camera permission to capture a live selfie for identity verification. Precise location data, on the other hand, is crucial for compliance with regulatory requirements, helping to prevent fraud by verifying the user's location. This combination of permissions ensures that our KYC procedures are both secure and user-friendly, providing peace of mind for both users and regulators.

### Add Permission in AndroidManifest.xml

Allow the app to ask for camera & location permissions.

```kotlin
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.CAMERA" />
```

### Apply WebView Settings

Apply settings to the webview to save user data, access location, run javascript code & access camera in webview.

```kotlin
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
```

### Add WebChromeClient For Permission Handling

Handling Camera and Location Permissions in WebView for KYC(Know Your Customer) Processing.

```kotlin
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
```

### Add WebViewClient For Url Handling

Ensure All URLs Open Within the Same WebView via Custom Redirection Handling.

```kotlin
    webView?.webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            // Handle URL redirection here
            return super.shouldOverrideUrlLoading(view, request)
        }
    }
```