import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.FinancialConnectionsSheetResultForLinkCallback
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetForLinkContract
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetLauncher

internal class FinancialConnectionsSheetForLinkLauncher(
    private val activityResultLauncher: ActivityResultLauncher<FinancialConnectionsSheetActivityArgs.ForLink>
) : FinancialConnectionsSheetLauncher {

    constructor(
        activity: ComponentActivity,
        callback: FinancialConnectionsSheetResultForLinkCallback,
    ) : this(
        activity.registerForActivityResult(
            FinancialConnectionsSheetForLinkContract(),
            callback::onFinancialConnectionsSheetResult,
        )
    )

    override fun present(configuration: FinancialConnectionsSheet.Configuration) {
        activityResultLauncher.launch(
            FinancialConnectionsSheetActivityArgs.ForLink(configuration),
        )
    }
}
