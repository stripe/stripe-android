package com.stripe.android.connections.launcher

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.fragment.app.Fragment
import com.stripe.android.connections.ConnectionsSheet
import com.stripe.android.connections.ConnectionsSheetContract
import com.stripe.android.connections.ConnectionsSheetResult
import com.stripe.android.connections.ConnectionsSheetResultCallback
import org.jetbrains.annotations.TestOnly

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
            callback.onConnectionsSheetResult(it.toExposedResult())
        }
    )

    constructor(
        fragment: Fragment,
        callback: ConnectionsSheetResultCallback
    ) : this(
        fragment.registerForActivityResult(
            ConnectionsSheetContract()
        ) {
            callback.onConnectionsSheetResult(it.toExposedResult())
        }
    )

    @TestOnly
    constructor(
        fragment: Fragment,
        registry: ActivityResultRegistry,
        callback: ConnectionsSheetResultCallback
    ) : this(
        fragment.registerForActivityResult(
            ConnectionsSheetContract(),
            registry
        ) {
            callback.onConnectionsSheetResult(it.toExposedResult())
        },
    )

    override fun present(configuration: ConnectionsSheet.Configuration) {
        activityResultLauncher.launch(
            ConnectionsSheetContract.Args.Default(
                configuration,
            )
        )
    }
}

private fun ConnectionsSheetContract.Result.toExposedResult(): ConnectionsSheetResult =
    when (this) {
        is ConnectionsSheetContract.Result.Canceled -> ConnectionsSheetResult.Canceled
        is ConnectionsSheetContract.Result.Failed -> ConnectionsSheetResult.Failed(error)
        is ConnectionsSheetContract.Result.Completed -> ConnectionsSheetResult.Completed(
            linkAccountSession = linkAccountSession,
        )
    }
