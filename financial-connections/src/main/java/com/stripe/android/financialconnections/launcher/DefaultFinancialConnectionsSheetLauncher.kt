package com.stripe.android.financialconnections.launcher

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.fragment.app.Fragment
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.FinancialConnectionsSheetContract
import com.stripe.android.financialconnections.FinancialConnectionsSheetResult
import com.stripe.android.financialconnections.FinancialConnectionsSheetResultCallback
import org.jetbrains.annotations.TestOnly

internal class DefaultFinancialConnectionsSheetLauncher(
    private val activityResultLauncher: ActivityResultLauncher<FinancialConnectionsSheetContract.Args>
) : FinancialConnectionsSheetLauncher {

    constructor(
        activity: ComponentActivity,
        callback: FinancialConnectionsSheetResultCallback
    ) : this(
        activity.registerForActivityResult(
            FinancialConnectionsSheetContract()
        ) {
            callback.onFinancialConnectionsSheetResult(it.toExposedResult())
        }
    )

    constructor(
        fragment: Fragment,
        callback: FinancialConnectionsSheetResultCallback
    ) : this(
        fragment.registerForActivityResult(
            FinancialConnectionsSheetContract()
        ) {
            callback.onFinancialConnectionsSheetResult(it.toExposedResult())
        }
    )

    @TestOnly
    constructor(
        fragment: Fragment,
        registry: ActivityResultRegistry,
        callback: FinancialConnectionsSheetResultCallback
    ) : this(
        fragment.registerForActivityResult(
            FinancialConnectionsSheetContract(),
            registry
        ) {
            callback.onFinancialConnectionsSheetResult(it.toExposedResult())
        },
    )

    override fun present(configuration: FinancialConnectionsSheet.Configuration) {
        activityResultLauncher.launch(
            FinancialConnectionsSheetContract.Args.Default(
                configuration,
            )
        )
    }
}

private fun FinancialConnectionsSheetContract.Result.toExposedResult(): FinancialConnectionsSheetResult =
    when (this) {
        is FinancialConnectionsSheetContract.Result.Canceled -> FinancialConnectionsSheetResult.Canceled
        is FinancialConnectionsSheetContract.Result.Failed -> FinancialConnectionsSheetResult.Failed(error)
        is FinancialConnectionsSheetContract.Result.Completed -> FinancialConnectionsSheetResult.Completed(
            linkAccountSession = linkAccountSession,
        )
    }
