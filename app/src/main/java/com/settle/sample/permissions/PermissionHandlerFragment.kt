package com.settle.sample.permissions

import android.Manifest
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.Intent.FLAG_ACTIVITY_NO_HISTORY
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.view.View
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.material.snackbar.Snackbar


class PermissionHandlerFragment : Fragment() {
    private var allowPermissionMsg = ""
    private var goToSettingsMsg = ""
    private var allowPermissionBtnText = ""
    private var permissionType: PermissionType? = null

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                setCameraPermissionResult(permissionGranted = true)
            } else {
                when {
                    shouldShowPermissionRationale(permission = Manifest.permission.CAMERA) -> {
                        createSnackBar(message = allowPermissionMsg) {
                            requestCameraPermission()
                        }
                    }
                    else -> {
                        createSnackBar(message = goToSettingsMsg) {
                            openApplicationSettings()
                            // Since from application we won't be getting any response close this fragment
                            setCameraPermissionResult(permissionGranted = false)
                        }
                    }
                }
            }
        }

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.all { it.value }) {
                checkIfGpsEnabled()
            } else {
                when {
                    shouldShowPermissionRationale(permission = Manifest.permission.ACCESS_COARSE_LOCATION) &&
                            shouldShowPermissionRationale(permission = Manifest.permission.ACCESS_FINE_LOCATION) -> {
                        createSnackBar(message = allowPermissionMsg) { requestLocationPermission() }
                    }
                    else -> {
                        createSnackBar(message = goToSettingsMsg) {
                            openApplicationSettings()
                            // Since from application we won't be getting any response close this fragment
//                            setLocationPermissionResult(permissionGranted = false)
                        }
                    }
                }
            }
        }

    private val gpsResolutionForResult =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { activityResult ->
            if (activityResult.resultCode == RESULT_OK) {
                setLocationPermissionResult(permissionGranted = true)
            } else {
                setLocationPermissionResult(permissionGranted = false)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            allowPermissionMsg = it.getString(ALLOW_PERMISSION_MSG_KEY, "")
            goToSettingsMsg = it.getString(OPEN_SETTINGS_MSG_KEY, "")
            allowPermissionBtnText = it.getString(ALLOW_PERMISSION_BTN_TEXT_KEY, "")
            permissionType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getSerializable(PERMISSION_TYPE_KEY, PermissionType::class.java)
            } else {
                it.getSerializable(PERMISSION_TYPE_KEY) as? PermissionType
            }
        }

        requestPermission()
    }

    private fun requestPermission() {
        when (permissionType) {
            PermissionType.Camera -> {
                requestCameraPermission()
            }
            PermissionType.Location -> {
                requestLocationPermission()
            }
            null -> {
                // Do nothing.
            }
        }
    }

    //region camera permission
    private fun requestCameraPermission() {
        if (isCameraPermissionGranted) {
            // Camera permission already granted
            setCameraPermissionResult(permissionGranted = true)
        } else {
            // Ask for camera permission
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private val isCameraPermissionGranted: Boolean
        get() {
            return ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        }

    private fun setCameraPermissionResult(permissionGranted: Boolean) {
        setFragmentResult(
            requestKey = RESULT_PERMISSION_KEY,
            result = Bundle().apply {
                putSerializable(RESULT_PERMISSION_TYPE_KEY, permissionType)
                putSerializable(RESULT_PERMISSION_GRANTED_KEY, permissionGranted)
            }
        )
        removeFragmentFromActivity()
    }
    //endregion

    //region location permission
    private fun requestLocationPermission() {
        if (isLocationPermissionGranted) {
            checkIfGpsEnabled()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }

    private fun setLocationPermissionResult(permissionGranted: Boolean) {
        setFragmentResult(
            requestKey = RESULT_PERMISSION_KEY,
            result = Bundle().apply {
                putSerializable(RESULT_PERMISSION_TYPE_KEY, permissionType)
                putBoolean(RESULT_PERMISSION_GRANTED_KEY, permissionGranted)
            }
        )
        removeFragmentFromActivity()
    }

    private fun checkIfGpsEnabled() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            DEFAULT_GPS_INTERVAL_MILLIS
        ).build()

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client = LocationServices.getSettingsClient(requireActivity())
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            setLocationPermissionResult(permissionGranted = true)
        }.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    val intentSenderRequest =
                        IntentSenderRequest.Builder(exception.resolution).build()
                    gpsResolutionForResult.launch(intentSenderRequest)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                    setLocationPermissionResult(permissionGranted = false)
                    Toast.makeText(requireContext(), sendEx.message.toString(), Toast.LENGTH_LONG)
                        .show()
                }
            }
        }
    }

    private val isLocationPermissionGranted: Boolean
        get() {
            val fineLocationPermission = Manifest.permission.ACCESS_FINE_LOCATION
            val coarseLocationPermission = Manifest.permission.ACCESS_COARSE_LOCATION

            val fineLocationPermissionGranted = ContextCompat.checkSelfPermission(
                requireContext(),
                fineLocationPermission
            ) == PackageManager.PERMISSION_GRANTED

            val coarseLocationPermissionGranted = ContextCompat.checkSelfPermission(
                requireContext(),
                coarseLocationPermission
            ) == PackageManager.PERMISSION_GRANTED

            return fineLocationPermissionGranted && coarseLocationPermissionGranted
        }
    //endregion

    //region snack bar handling

    /**
     * Display info to the user why the permission is required
     */
    private fun createSnackBar(
        message: String,
        action: () -> Unit,
    ) {
        val rootView: View = activity?.findViewById<View>(android.R.id.content)?.getRootView()
            ?: activity?.window?.decorView?.findViewById(android.R.id.content) ?: return

        Snackbar.make(
            rootView,
            message,
            Snackbar.LENGTH_INDEFINITE
        ).apply {
            setAction(allowPermissionBtnText) {
                action.invoke()
            }
            setAnchorView(android.R.id.navigationBarBackground)
            show()
        }
    }
    //endregion

    //region utils

    /**
     * Go to application settings screen and allow permissions
     */
    private fun openApplicationSettings() {
        Intent(ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts(PACKAGE, activity?.packageName, null)
            addFlags(FLAG_ACTIVITY_NEW_TASK)
            addFlags(FLAG_ACTIVITY_NO_HISTORY)
            addFlags(FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            startActivity(this)
        }
    }

    private fun removeFragmentFromActivity() {
        activity
            ?.supportFragmentManager
            ?.beginTransaction()
            ?.remove(this)
            ?.commitNow()
    }

    private fun shouldShowPermissionRationale(permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(
            requireActivity(),
            permission
        )
    }
    //endregion

    companion object {
        private const val ALLOW_PERMISSION_MSG_KEY = "allowPermissionMsgKey"
        private const val PERMISSION_TYPE_KEY = "permissionTypeKey"

        private const val ALLOW_CAMERA_MSG_STRING = "We need camera permission to take your selfie!"
        private const val ALLOW_LOCATION_MSG_STRING =
            "We need location permission to verify your address!"

        private const val OPEN_SETTINGS_MSG_KEY = "openSettingsMsgKey"
        private const val OPEN_SETTINGS_MSG_STRING = "Kindly, allow the permissions from settings!"

        private const val ALLOW_PERMISSION_BTN_TEXT_KEY = "allowPermissionBtnTextKey"
        private const val ALLOW_PERMISSION_BTN_TEXT_STRING = "Allow"

        const val RESULT_PERMISSION_TYPE_KEY = "permissionResultTypeKey"
        const val RESULT_PERMISSION_GRANTED_KEY = "permissionGrantedKey"
        const val RESULT_PERMISSION_KEY = "permissionResultKey"

        private const val DEFAULT_GPS_INTERVAL_MILLIS = 5000L

        private const val PACKAGE = "package"

        fun cameraPermissionInstance(
            allowCameraMsg: String = ALLOW_CAMERA_MSG_STRING,
            allowPermissionBtnText: String = ALLOW_PERMISSION_BTN_TEXT_STRING,
            openSettingsMsg: String = OPEN_SETTINGS_MSG_STRING,
        ): PermissionHandlerFragment {
            return PermissionHandlerFragment().apply {
                arguments = Bundle().apply {
                    putString(ALLOW_PERMISSION_MSG_KEY, allowCameraMsg)
                    putString(ALLOW_PERMISSION_BTN_TEXT_KEY, allowPermissionBtnText)
                    putString(OPEN_SETTINGS_MSG_KEY, openSettingsMsg)
                    putSerializable(PERMISSION_TYPE_KEY, PermissionType.Camera)
                }
            }
        }

        fun locationPermissionInstance(
            allowLocationMsg: String = ALLOW_LOCATION_MSG_STRING,
            allowPermissionBtnText: String = ALLOW_PERMISSION_BTN_TEXT_STRING,
            openSettingsMsg: String = OPEN_SETTINGS_MSG_STRING,
        ): PermissionHandlerFragment {
            return PermissionHandlerFragment().apply {
                arguments = Bundle().apply {
                    putString(ALLOW_PERMISSION_MSG_KEY, allowLocationMsg)
                    putString(ALLOW_PERMISSION_BTN_TEXT_KEY, allowPermissionBtnText)
                    putString(OPEN_SETTINGS_MSG_KEY, openSettingsMsg)
                    putSerializable(PERMISSION_TYPE_KEY, PermissionType.Location)
                }
            }
        }
    }
}