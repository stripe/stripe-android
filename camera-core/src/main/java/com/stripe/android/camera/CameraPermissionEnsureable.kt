package com.stripe.android.camera

import androidx.annotation.RestrictTo

/**
 * Indicates this class is able to check camera permission and handles the results accordingly.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface CameraPermissionEnsureable {
    /**
     * Checks if current app has camera permission, prompts the user to grant permission if needed.
     * Invokes [onCameraReady] when the app already has permission or user grants permission.
     * Invokes [onUserDeniedCameraPermission] if user denied the permission.
     */
    fun ensureCameraPermission(
        onCameraReady: () -> Unit,
        onUserDeniedCameraPermission: () -> Unit
    )
}
