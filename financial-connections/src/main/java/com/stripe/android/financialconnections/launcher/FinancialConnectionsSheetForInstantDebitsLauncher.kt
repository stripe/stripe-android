package com.stripe.android.financialconnections.launcher

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.annotation.RestrictTo
import androidx.fragment.app.Fragment
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import org.jetbrains.annotations.TestOnly

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

    @TestOnly
    internal constructor(
        fragment: Fragment,
        registry: ActivityResultRegistry,
        callback: (FinancialConnectionsSheetInstantDebitsResult) -> Unit
    ) : this(
        fragment.registerForActivityResult(
            FinancialConnectionsSheetForInstantDebitsContract(),
            registry,
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
