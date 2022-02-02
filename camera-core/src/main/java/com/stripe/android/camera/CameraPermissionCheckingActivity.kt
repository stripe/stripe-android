package com.stripe.android.camera

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.annotation.RestrictTo
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.stripe.android.camera.framework.Stats
import com.stripe.android.core.storage.StorageFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class CameraPermissionCheckingActivity : AppCompatActivity() {

    /**
     * Prepare to start the camera. Once the camera is ready, [onCameraReady] must be called.
     */
    protected abstract fun prepareCamera(onCameraReady: () -> Unit)

    /**
     * Callback when camera is ready.
     */
    protected abstract fun onCameraReady()

    /**
     * The camera permission was denied.
     */
    protected abstract fun userDeniedCameraPermission()

    private val storage by lazy {
        StorageFactory.getStorageInstance(this, PERMISSION_STORAGE_NAME)
    }

    private val mainScope = CoroutineScope(Dispatchers.Main)

    private val permissionStat = Stats.trackTask(PERMISSION_STATS_TRACK_NAME)

    /**
     * Check the camera permission, invokes [onCameraReady] upon permission grant,
     * invokes [userDeniedCameraPermission] otherwise.
     */
    protected fun ensureCameraPermission() = when {
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED -> {
            mainScope.launch { permissionStat.trackResult("success") }
            prepareCamera { onCameraReady() }
        }
        ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            Manifest.permission.CAMERA,
        ) -> showPermissionRationaleDialog()
        storage.getBoolean(
            PERMISSION_RATIONALE_SHOWN,
            false,
        ) -> showPermissionDeniedDialog()
        else -> requestCameraPermission()
    }

    /**
     * Handle permission status changes. If the camera permission has been granted, start it. If
     * not, show a dialog.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty()) {
            when (grantResults[0]) {
                PackageManager.PERMISSION_GRANTED -> {
                    mainScope.launch { permissionStat.trackResult("success") }
                    prepareCamera { onCameraReady() }
                }
                else -> {
                    mainScope.launch { permissionStat.trackResult("failure") }
                    userDeniedCameraPermission()
                }
            }
        }
    }

    /**
     * Show an explanation dialog for why we are requesting camera permissions.
     */
    protected open fun showPermissionRationaleDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(R.string.stripe_camera_permission_denied_message)
            .setPositiveButton(R.string.stripe_camera_permission_denied_ok) { _, _ ->
                requestCameraPermission()
            }
        builder.show()
        storage.storeValue(PERMISSION_RATIONALE_SHOWN, true)
    }

    /**
     * Show an explanation dialog for why we are requesting camera permissions when the permission
     * has been permanently denied.
     */
    protected open fun showPermissionDeniedDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(R.string.stripe_camera_permission_denied_message)
            .setPositiveButton(R.string.stripe_camera_permission_denied_ok) { _, _ ->
                storage.storeValue(PERMISSION_RATIONALE_SHOWN, false)
                openAppSettings(this)
            }
            .setNegativeButton(R.string.stripe_camera_permission_denied_cancel) { _, _ ->
                userDeniedCameraPermission()
            }
        builder.show()
    }

    /**
     * Request permission to use the camera.
     */
    protected open fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            PERMISSION_REQUEST_CODE,
        )
    }

    /**
     * Open the settings for this app
     */
    protected open fun openAppSettings(activity: Activity) {
        val intent = Intent()
            .setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.fromParts("package", activity.applicationContext.packageName, null))
        activity.startActivity(intent)
    }

    private companion object {
        private const val PERMISSION_STORAGE_NAME = "scan_camera_permissions"
        private const val PERMISSION_STATS_TRACK_NAME = "camera_permission"
        private const val PERMISSION_REQUEST_CODE = 1200
        private const val PERMISSION_RATIONALE_SHOWN = "permission_rationale_shown"
    }
}
