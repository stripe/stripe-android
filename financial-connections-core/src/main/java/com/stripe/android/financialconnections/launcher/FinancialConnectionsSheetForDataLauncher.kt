package com.stripe.android.financialconnections.launcher

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import com.stripe.android.financialconnections.ElementsSessionContext
import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration
import com.stripe.android.financialconnections.FinancialConnectionsSheetResultCallback
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs.ForData
import org.jetbrains.annotations.TestOnly

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class FinancialConnectionsSheetForDataLauncher(
    @get:VisibleForTesting
    val activityResultLauncher: ActivityResultLauncher<ForData>
) : FinancialConnectionsSheetLauncher {

    constructor(
        activity: ComponentActivity,
        intentBuilder: (FinancialConnectionsSheetActivityArgs) -> Intent,
        callback: FinancialConnectionsSheetResultCallback
    ) : this(
        activity.registerForActivityResult(
            FinancialConnectionsSheetForDataContract(intentBuilder)
        ) {
            callback.onFinancialConnectionsSheetResult(it)
        }
    )

    constructor(
        fragment: Fragment,
        intentBuilder: (FinancialConnectionsSheetActivityArgs) -> Intent,
        callback: FinancialConnectionsSheetResultCallback
    ) : this(
        fragment.registerForActivityResult(
            FinancialConnectionsSheetForDataContract(intentBuilder)
        ) {
            callback.onFinancialConnectionsSheetResult(it)
        }
    )

    @TestOnly
    constructor(
        fragment: Fragment,
        registry: ActivityResultRegistry,
        intentBuilder: (FinancialConnectionsSheetActivityArgs) -> Intent,
        callback: FinancialConnectionsSheetResultCallback
    ) : this(
        fragment.registerForActivityResult(
            FinancialConnectionsSheetForDataContract(intentBuilder),
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
            ForData(
                configuration = configuration,
                elementsSessionContext = elementsSessionContext,
            )
        )
    }
}
