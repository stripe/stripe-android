package com.stripe.android.identity.navigation

import androidx.fragment.app.Fragment
import com.stripe.android.identity.IdentityActivity

/**
 * Gets the hosting [IdentityActivity] and invokes [IdentityActivity.ensureCameraPermission].
 */
fun Fragment.ensureCameraPermissionFromIdentityActivity(
    onCameraReady: () -> Unit,
    onUserDeniedCameraPermission: () -> Unit
) {
    requireNotNull(activity as? IdentityActivity) {
        "Hosting activity is not IdentityActivity."
    }.ensureCameraPermission(
        onCameraReady,
        onUserDeniedCameraPermission
    )
}
