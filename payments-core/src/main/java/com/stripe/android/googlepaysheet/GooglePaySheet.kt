package com.stripe.android.googlepaysheet

import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment

/**
 * A drop-in class that presents a Google Pay sheet to collect a customer's payment.
 */
internal class GooglePaySheet(
    private val googlePaySheetLauncher: GooglePaySheetLauncher
) {
    /**
     * Constructor to be used when launching GooglePaySheet from an Activity.
     *
     * @param activity the Activity that is launching the GooglePaySheet
     * @param callback called with the result of the GooglePaySheet operation
     */
    internal constructor(
        activity: ComponentActivity,
        callback: GooglePaySheetResultCallback
    ) : this(
        DefaultGooglePaySheetLauncher(activity, callback)
    )

    /**
     * Constructor to be used when launching GooglePaySheet from an Activity.
     *
     * @param fragment the Fragment that is launching the GooglePaySheet
     * @param callback called with the result of the GooglePaySheet operation
     */
    internal constructor(
        fragment: Fragment,
        callback: GooglePaySheetResultCallback
    ) : this(
        DefaultGooglePaySheetLauncher(fragment, callback)
    )
}
