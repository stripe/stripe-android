package com.stripe.android.camera

import androidx.annotation.RestrictTo

/**
 * Indicates this class is able to open the app's settings.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface AppSettingsOpenable {
    fun openAppSettings()
}
