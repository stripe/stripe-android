package com.stripe.android.financialconnections.launcher

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.fragment.app.Fragment
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.FinancialConnectionsSheetResultForTokenCallback
import org.jetbrains.annotations.TestOnly

internal class FinancialConnectionsSheetForTokenLauncher(
    private val activityResultLauncher: ActivityResultLauncher<FinancialConnectionsSheetActivityArgs.ForToken>
) : FinancialConnectionsSheetLauncher {

    constructor(
        activity: ComponentActivity,
        callback: FinancialConnectionsSheetResultForTokenCallback
    ) : this(
        activity.registerForActivityResult(
            FinancialConnectionsSheetForTokenContract()
        ) {
            callback.onFinancialConnectionsSheetResult(it)
        }
    )

    constructor(
        fragment: Fragment,
        callback: FinancialConnectionsSheetResultForTokenCallback
    ) : this(
        fragment.registerForActivityResult(
            FinancialConnectionsSheetForTokenContract()
        ) {
            callback.onFinancialConnectionsSheetResult(it)
        }
    )

    @TestOnly
    constructor(
        fragment: Fragment,
        registry: ActivityResultRegistry,
        callback: FinancialConnectionsSheetResultForTokenCallback
    ) : this(
        fragment.registerForActivityResult(
            FinancialConnectionsSheetForTokenContract(),
            registry
        ) {
            callback.onFinancialConnectionsSheetResult(it)
        }
    )

    override fun present(
        configuration: FinancialConnectionsSheet.Configuration,
        elementsSessionContext: FinancialConnectionsSheet.ElementsSessionContext?
    ) {
        activityResultLauncher.launch(
            FinancialConnectionsSheetActivityArgs.ForToken(
                configuration = configuration,
                elementsSessionContext = elementsSessionContext,
            )
        )
    }
}
