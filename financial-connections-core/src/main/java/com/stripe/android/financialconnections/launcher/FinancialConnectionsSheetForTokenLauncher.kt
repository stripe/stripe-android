package com.stripe.android.financialconnections.launcher

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.annotation.RestrictTo
import androidx.fragment.app.Fragment
import com.stripe.android.financialconnections.ElementsSessionContext
import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration
import com.stripe.android.financialconnections.FinancialConnectionsSheetResultForTokenCallback
import org.jetbrains.annotations.TestOnly

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class FinancialConnectionsSheetForTokenLauncher(
    private val activityResultLauncher: ActivityResultLauncher<FinancialConnectionsSheetActivityArgs.ForToken>
) : FinancialConnectionsSheetLauncher {

    constructor(
        activity: ComponentActivity,
        intentBuilder: (FinancialConnectionsSheetActivityArgs) -> Intent,
        callback: FinancialConnectionsSheetResultForTokenCallback
    ) : this(
        activity.registerForActivityResult(
            FinancialConnectionsSheetForTokenContract(intentBuilder)
        ) {
            callback.onFinancialConnectionsSheetResult(it)
        }
    )

    constructor(
        fragment: Fragment,
        intentBuilder: (FinancialConnectionsSheetActivityArgs) -> Intent,
        callback: FinancialConnectionsSheetResultForTokenCallback
    ) : this(
        fragment.registerForActivityResult(
            FinancialConnectionsSheetForTokenContract(intentBuilder)
        ) {
            callback.onFinancialConnectionsSheetResult(it)
        }
    )

    @TestOnly
    constructor(
        fragment: Fragment,
        registry: ActivityResultRegistry,
        intentBuilder: (FinancialConnectionsSheetActivityArgs) -> Intent,
        callback: FinancialConnectionsSheetResultForTokenCallback
    ) : this(
        fragment.registerForActivityResult(
            FinancialConnectionsSheetForTokenContract(intentBuilder),
            registry
        ) {
            callback.onFinancialConnectionsSheetResult(it)
        }
    )

    override fun present(
        configuration: FinancialConnectionsSheetConfiguration,
        elementsSessionContext: ElementsSessionContext?
    ) {
        activityResultLauncher.launch(
            FinancialConnectionsSheetActivityArgs.ForToken(
                configuration = configuration,
                elementsSessionContext = elementsSessionContext,
            )
        )
    }
}
