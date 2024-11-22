package com.stripe.android.financialconnections.launcher

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.annotation.RestrictTo
import androidx.fragment.app.Fragment
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.FinancialConnectionsSheetInternalResultCallback
import org.jetbrains.annotations.TestOnly

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class FinancialConnectionsSheetForDataLauncher(
    private val activityResultLauncher: ActivityResultLauncher<FinancialConnectionsSheetActivityArgs.ForData>
) : FinancialConnectionsSheetLauncher {

    constructor(
        activity: ComponentActivity,
        callback: FinancialConnectionsSheetInternalResultCallback
    ) : this(
        activity.registerForActivityResult(
            FinancialConnectionsSheetForDataContract()
        ) {
            callback.onFinancialConnectionsSheetResult(it)
        }
    )

    constructor(
        fragment: Fragment,
        callback: FinancialConnectionsSheetInternalResultCallback
    ) : this(
        fragment.registerForActivityResult(
            FinancialConnectionsSheetForDataContract()
        ) {
            callback.onFinancialConnectionsSheetResult(it)
        }
    )

    @TestOnly
    constructor(
        fragment: Fragment,
        registry: ActivityResultRegistry,
        callback: FinancialConnectionsSheetInternalResultCallback
    ) : this(
        fragment.registerForActivityResult(
            FinancialConnectionsSheetForDataContract(),
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
            FinancialConnectionsSheetActivityArgs.ForData(
                configuration = configuration,
                elementsSessionContext = elementsSessionContext,
            )
        )
    }
}
