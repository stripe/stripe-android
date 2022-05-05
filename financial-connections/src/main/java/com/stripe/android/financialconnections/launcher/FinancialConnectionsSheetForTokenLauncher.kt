package com.stripe.android.financialconnections.launcher

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.annotation.RestrictTo
import androidx.fragment.app.Fragment
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.FinancialConnectionsSheetContract
import com.stripe.android.financialconnections.FinancialConnectionsSheetContract.Result
import com.stripe.android.financialconnections.FinancialConnectionsSheetContract.Result.Canceled
import com.stripe.android.financialconnections.FinancialConnectionsSheetContract.Result.Completed
import com.stripe.android.financialconnections.FinancialConnectionsSheetContract.Result.Failed
import com.stripe.android.financialconnections.FinancialConnectionsSheetForTokenResult
import org.jetbrains.annotations.TestOnly

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class FinancialConnectionsSheetForTokenLauncher(
    private val activityResultLauncher: ActivityResultLauncher<FinancialConnectionsSheetContract.Args>
) : FinancialConnectionsSheetLauncher {

    constructor(
        activity: ComponentActivity,
        callback: (FinancialConnectionsSheetForTokenResult) -> Unit
    ) : this(
        activity.registerForActivityResult(
            FinancialConnectionsSheetContract()
        ) {
            callback(it.toExposedResult())
        }
    )

    constructor(
        fragment: Fragment,
        callback: (FinancialConnectionsSheetForTokenResult) -> Unit
    ) : this(
        fragment.registerForActivityResult(
            FinancialConnectionsSheetContract()
        ) {
            callback(it.toExposedResult())
        }
    )

    @TestOnly
    constructor(
        fragment: Fragment,
        registry: ActivityResultRegistry,
        callback: (FinancialConnectionsSheetForTokenResult) -> Unit
    ) : this(
        fragment.registerForActivityResult(
            FinancialConnectionsSheetContract(),
            registry
        ) {
            callback(it.toExposedResult())
        },
    )

    override fun present(configuration: FinancialConnectionsSheet.Configuration) {
        activityResultLauncher.launch(
            FinancialConnectionsSheetContract.Args.ForToken(
                configuration,
            )
        )
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) companion object {
        fun Result.toExposedResult(): FinancialConnectionsSheetForTokenResult {
            return when (this) {
                is Canceled -> FinancialConnectionsSheetForTokenResult.Canceled
                is Failed -> FinancialConnectionsSheetForTokenResult.Failed(error)
                is Completed -> FinancialConnectionsSheetForTokenResult.Completed(
                    financialConnectionsSession = financialConnectionsSession,
                    token = requireNotNull(token)
                )
            }
        }
    }
}
