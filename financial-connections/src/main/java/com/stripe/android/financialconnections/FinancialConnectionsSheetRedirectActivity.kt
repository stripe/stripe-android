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
            // link-accounts hosts:
            host == "link-accounts" -> {
                when {
                    // Redirect from app2app finish back to SDK.
                    // TODO@carlosmuvi check if the current flow is native or web from the deeplink
                    this.toString().contains("authentication_return") -> Intent(
                        this@FinancialConnectionsSheetRedirectActivity,
                        FinancialConnectionsSheetNativeActivity::class.java
                    )
                    // redirect from embedded AuthFlow completed on web back to SDK.
                    else -> Intent(
                        this@FinancialConnectionsSheetRedirectActivity,
                        FinancialConnectionsSheetActivity::class.java
                    )
                }
            }

            // native-redirect hosts:
            // redirections from embedded web AuthFlow back SDK (app2app start)
            host == "native-redirect" -> Intent(
                this@FinancialConnectionsSheetRedirectActivity,
                FinancialConnectionsSheetActivity::class.java
            )

            else -> null
        }
}

private fun Uri.isFinancialConnectionsScheme(): Boolean {
    return (this.scheme == "stripe-auth" || this.scheme == "stripe")
}
