package com.stripe.android.connections

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment

internal class DefaultConnectionsSheetLauncher(
    private val activityResultLauncher: ActivityResultLauncher<ConnectionsSheetContract.Args>
) : ConnectionsSheetLauncher {

    constructor(
        activity: ComponentActivity,
        callback: ConnectionsSheetResultCallback
    ) : this(
        activity.registerForActivityResult(
            ConnectionsSheetContract()
        ) {
            callback.onConnectionsSheetResult(it)
        }
    )

    constructor(
        fragment: Fragment,
        callback: ConnectionsSheetResultCallback
    ) : this(
        fragment.registerForActivityResult(
            ConnectionsSheetContract()
        ) {
            callback.onConnectionsSheetResult(it)
        }
    )

    override fun present(configuration: ConnectionsSheet.Configuration) {
        activityResultLauncher.launch(
            ConnectionsSheetContract.Args(
                configuration,
            )
        )
    }
}
