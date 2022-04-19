package com.stripe.android.financialconnections

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment

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
            callback.onFinancialConnectionsSheetResult(it)
        }
    )

    constructor(
        fragment: Fragment,
        callback: FinancialConnectionsSheetResultCallback
    ) : this(
        fragment.registerForActivityResult(
            FinancialConnectionsSheetContract()
        ) {
            callback.onFinancialConnectionsSheetResult(it)
        }
    )

    override fun present(configuration: FinancialConnectionsSheet.Configuration) {
        activityResultLauncher.launch(
            FinancialConnectionsSheetContract.Args(
                configuration,
            )
        )
    }
}
