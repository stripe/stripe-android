package com.stripe.android.financialconnections.lite

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity

/**
 * This Activity handles redirects from the Institution authorization flow hand-off from native.
 * It'll process the result url in [Intent.getData] and pass them back to the opening activity.
 */
internal class FinancialConnectionsSheetLiteRedirectActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /**
         * Used together, FLAG_ACTIVITY_SINGLE_TOP and FLAG_ACTIVITY_CLEAR_TOP
         * clear everything on the stack above the opening activity, including CCT.
         */
        intent.data?.let { uri ->
            uri.toIntent()
                ?.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                ?.also { it.data = uri }
                ?.let { startActivity(it) }
        }
    }

    /**
     * @return Intent, or null if deeplink cannot be mapped to an Intent (in case of unknown deep links).
     */
    private fun Uri.toIntent(): Intent? = when {
        isFinancialConnectionsScheme().not() -> null
        else -> FinancialConnectionsSheetLiteActivity::class.java
    }?.let { destinationActivity ->
        Intent(
            this@FinancialConnectionsSheetLiteRedirectActivity,
            destinationActivity
        )
    }

    private fun Uri.isFinancialConnectionsScheme(): Boolean {
        return (this.scheme == "stripe")
    }
}
