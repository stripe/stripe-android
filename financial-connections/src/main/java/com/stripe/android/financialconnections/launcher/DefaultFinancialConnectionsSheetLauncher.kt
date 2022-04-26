package com.stripe.android.financialconnections.launcher

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP
import androidx.fragment.app.Fragment
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.FinancialConnectionsSheetContract
import com.stripe.android.financialconnections.FinancialConnectionsSheetResult
import com.stripe.android.financialconnections.FinancialConnectionsSheetResult.Canceled
import com.stripe.android.financialconnections.FinancialConnectionsSheetResult.Completed
import com.stripe.android.financialconnections.FinancialConnectionsSheetResult.Failed
import com.stripe.android.financialconnections.FinancialConnectionsSheetResultCallback
import org.jetbrains.annotations.TestOnly

@RestrictTo(LIBRARY_GROUP)
class DefaultFinancialConnectionsSheetLauncher(
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

    companion object {
        @RestrictTo(LIBRARY_GROUP)
        fun FinancialConnectionsSheetContract.Result.toExposedResult(): FinancialConnectionsSheetResult {
            return when (this) {
                is FinancialConnectionsSheetContract.Result.Canceled -> Canceled
                is FinancialConnectionsSheetContract.Result.Failed -> Failed(error)
                is FinancialConnectionsSheetContract.Result.Completed -> Completed(
                    linkAccountSession = linkAccountSession,
                )
            }
        }
    }
}
