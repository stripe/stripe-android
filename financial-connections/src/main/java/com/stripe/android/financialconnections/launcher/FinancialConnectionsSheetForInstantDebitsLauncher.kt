package com.stripe.android.financialconnections.launcher

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RestrictTo
import com.stripe.android.financialconnections.FinancialConnectionsSheet

@Suppress("unused")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class FinancialConnectionsSheetForInstantDebitsLauncher(
    private val activityResultLauncher: ActivityResultLauncher<FinancialConnectionsSheetActivityArgs.ForInstantDebits>
) : FinancialConnectionsSheetLauncher {

    constructor(
        activity: ComponentActivity,
        callback: (FinancialConnectionsSheetInstantDebitsResult) -> Unit
    ) : this(
        activity.registerForActivityResult(
            FinancialConnectionsSheetForInstantDebitsContract(),
            callback::invoke
        )
    )

    override fun present(configuration: FinancialConnectionsSheet.Configuration) {
        activityResultLauncher.launch(
            FinancialConnectionsSheetActivityArgs.ForInstantDebits(
                configuration
            )
        )
    }
}
