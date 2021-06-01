package com.stripe.android.googlepaysheet

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.fragment.app.Fragment
import org.jetbrains.annotations.TestOnly

internal interface GooglePaySheetLauncher

internal class DefaultGooglePaySheetLauncher(
    private val activityResultLauncher: ActivityResultLauncher<StripeGooglePayContract.Args>
) : GooglePaySheetLauncher {

    constructor(
        activity: ComponentActivity,
        callback: GooglePaySheetResultCallback
    ) : this(
        activity.registerForActivityResult(
            StripeGooglePayContract()
        ) {
            callback.onResult(it)
        }
    )

    constructor(
        fragment: Fragment,
        callback: GooglePaySheetResultCallback
    ) : this(
        fragment.registerForActivityResult(
            StripeGooglePayContract()
        ) {
            callback.onResult(it)
        }
    )

    @TestOnly
    constructor(
        fragment: Fragment,
        registry: ActivityResultRegistry,
        callback: GooglePaySheetResultCallback
    ) : this(
        fragment.registerForActivityResult(
            StripeGooglePayContract(),
            registry
        ) {
            callback.onResult(it)
        }
    )
}
