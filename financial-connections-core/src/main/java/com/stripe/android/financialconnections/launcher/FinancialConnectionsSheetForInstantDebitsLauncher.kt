package com.stripe.android.financialconnections.launcher

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.annotation.RestrictTo
import androidx.fragment.app.Fragment
import com.stripe.android.financialconnections.ElementsSessionContext
import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration
import org.jetbrains.annotations.TestOnly

@Suppress("unused")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class FinancialConnectionsSheetForInstantDebitsLauncher(
    private val activityResultLauncher: ActivityResultLauncher<FinancialConnectionsSheetActivityArgs.ForInstantDebits>
) : FinancialConnectionsSheetLauncher {

    constructor(
        activity: ComponentActivity,
        intentBuilder: (FinancialConnectionsSheetActivityArgs) -> Intent,
        callback: (FinancialConnectionsSheetInstantDebitsResult) -> Unit
    ) : this(
        activity.registerForActivityResult(
            FinancialConnectionsSheetForInstantDebitsContract(intentBuilder),
            callback::invoke
        )
    )

    @TestOnly
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    constructor(
        fragment: Fragment,
        registry: ActivityResultRegistry,
        intentBuilder: (FinancialConnectionsSheetActivityArgs) -> Intent,
        callback: (FinancialConnectionsSheetInstantDebitsResult) -> Unit
    ) : this(
        fragment.registerForActivityResult(
            FinancialConnectionsSheetForInstantDebitsContract(intentBuilder),
            registry,
            callback::invoke
        )
    )

    override fun present(
        configuration: FinancialConnectionsSheetConfiguration,
        elementsSessionContext: ElementsSessionContext?
    ) {
        activityResultLauncher.launch(
            FinancialConnectionsSheetActivityArgs.ForInstantDebits(configuration, elementsSessionContext)
        )
    }
}
