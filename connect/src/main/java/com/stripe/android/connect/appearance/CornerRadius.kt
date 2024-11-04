package com.stripe.android.connect.appearance

import android.os.Parcelable
import com.stripe.android.connect.PrivateBetaConnectSDK
import kotlinx.parcelize.Parcelize

@PrivateBetaConnectSDK
@Parcelize
class CornerRadius(
    /**
     * The general border radius used throughout the components in dp.
     */
    val base: Float? = null,

    /**
     * The corner radius used specifically for buttons in dp.
     */
    val button: Float? = null,

    /**
     * The corner radius used specifically for badges in dp.
     */
    val badge: Float? = null,

    /**
     * The corner radius used for overlays in dp.
     */
    val overlay: Float? = null,

    /**
     * The corner radius used for form elements in dp.
     */
    val form: Float? = null
) : Parcelable {
    internal companion object {
        internal val default = CornerRadius()
    }
}
