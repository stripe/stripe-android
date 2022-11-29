package com.stripe.android.financialconnections

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity

/**
 * This Activity handles redirects from the Institution authorization flow hand-off from native.
 * It'll process the result url in [Intent.getData] and pass them back to the opening activity.
 */
class FinancialConnectionsSheetRedirectActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /**
         * Used together, FLAG_ACTIVITY_SINGLE_TOP and FLAG_ACTIVITY_CLEAR_TOP
         * clear everything on the stack above the opening activity, including CCT.
         */
        intent.data
            ?.let { uri ->
                uri.toIntent()
                    ?.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    ?.also { it.data = intent.data }
                    ?.let { startActivity(it) }
            }
        finish()
    }

    /**
     * @return Intent, or null if deeplink cannot be mapped to an Intent (in case of unknown deep links).
     */
    private fun Uri.toIntent(): Intent? =
        when {
            isFinancialConnectionsScheme().not() -> null
            // auth-redirect hosts: redirections from Abstract Auth in web back to native SDK
            host == "auth-redirect" ->
                Intent(
                    this@FinancialConnectionsSheetRedirectActivity,
                    FinancialConnectionsSheetNativeActivity::class.java
                )
            // link-accounts hosts: redirections embedded web AuthFlow back to non-native SDK.
            host == "link-accounts" -> Intent(
                this@FinancialConnectionsSheetRedirectActivity,
                FinancialConnectionsSheetActivity::class.java
            )
            else -> null
        }
}

private fun Uri.isFinancialConnectionsScheme(): Boolean {
    return (this.scheme == "stripe-auth" || this.scheme == "stripe")
}
