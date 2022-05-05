package com.stripe.android.financialconnections.launcher

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.fragment.app.Fragment
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.FinancialConnectionsSheetForTokenResult
import com.stripe.android.financialconnections.FinancialConnectionsSheetResultCallback
import org.jetbrains.annotations.TestOnly

internal class FinancialConnectionsSheetForTokenLauncher(
    private val activityResultLauncher: ActivityResultLauncher<FinancialConnectionsSheetActivityArgs.ForToken>
) : FinancialConnectionsSheetLauncher {

    constructor(
        activity: ComponentActivity,
        callback: (FinancialConnectionsSheetForTokenResult) -> Unit
    ) : this(
        activity.registerForActivityResult(
            FinancialConnectionsSheetForTokenContract()
        ) {
            callback(it)
        }
    )

    constructor(
        fragment: Fragment,
        callback: (FinancialConnectionsSheetForTokenResult) -> Unit
    ) : this(
        fragment.registerForActivityResult(
            FinancialConnectionsSheetForTokenContract()
        ) {
            callback(it)
        }
    )

    @TestOnly
    constructor(
        fragment: Fragment,
        registry: ActivityResultRegistry,
        callback: (FinancialConnectionsSheetForTokenResult) -> Unit
    ) : this(
        fragment.registerForActivityResult(
            FinancialConnectionsSheetForTokenContract(),
            registry
        ) {
            callback(it)
        },
    )

    override fun present(configuration: FinancialConnectionsSheet.Configuration) {
        activityResultLauncher.launch(
            FinancialConnectionsSheetActivityArgs.ForToken(
                configuration,
            )
        )
    }
}
