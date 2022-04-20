package com.stripe.android.connections.launcher

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.fragment.app.Fragment
import com.stripe.android.connections.ConnectionsSheet
import com.stripe.android.connections.ConnectionsSheetContract
import com.stripe.android.connections.ConnectionsSheetContract.Result
import com.stripe.android.connections.ConnectionsSheetForTokenResult
import org.jetbrains.annotations.TestOnly

internal class ConnectionsSheetForTokenLauncher(
    private val activityResultLauncher: ActivityResultLauncher<ConnectionsSheetContract.Args>
) : ConnectionsSheetLauncher {

    constructor(
        activity: ComponentActivity,
        callback: (ConnectionsSheetForTokenResult) -> Unit
    ) : this(
        activity.registerForActivityResult(
            ConnectionsSheetContract()
        ) {
            callback(it.toExposedResult())
        }
    )

    constructor(
        fragment: Fragment,
        callback: (ConnectionsSheetForTokenResult) -> Unit
    ) : this(
        fragment.registerForActivityResult(
            ConnectionsSheetContract()
        ) {
            callback(it.toExposedResult())
        }
    )

    @TestOnly
    constructor(
        fragment: Fragment,
        registry: ActivityResultRegistry,
        callback: (ConnectionsSheetForTokenResult) -> Unit
    ) : this(
        fragment.registerForActivityResult(
            ConnectionsSheetContract(),
            registry
        ) {
            callback(it.toExposedResult())
        },
    )

    override fun present(configuration: ConnectionsSheet.Configuration) {
        activityResultLauncher.launch(
            ConnectionsSheetContract.Args.ForToken(
                configuration,
            )
        )
    }
}

private fun Result.toExposedResult(): ConnectionsSheetForTokenResult = when (this) {
    is Result.Canceled -> ConnectionsSheetForTokenResult.Canceled
    is Result.Failed -> ConnectionsSheetForTokenResult.Failed(error)
    is Result.Completed -> ConnectionsSheetForTokenResult.Completed(
        linkAccountSession = linkAccountSession,
        token = requireNotNull(token)
    )
}
