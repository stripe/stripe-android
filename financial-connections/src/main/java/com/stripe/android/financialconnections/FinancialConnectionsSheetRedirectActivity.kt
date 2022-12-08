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
            host == "auth-redirect" -> FinancialConnectionsSheetNativeActivity::class.java

            host == "link-accounts" -> when {
                // link-accounts/.../authentication_return: Redirect from app2app finish to SDK.
                toString().contains("authentication_return") -> {
                    // TODO@carlosmuvi check if the current flow is native or web from the deeplink
                    val native = true
                    if (native) {
                        FinancialConnectionsSheetNativeActivity::class.java
                    } else {
                        FinancialConnectionsSheetActivity::class.java
                    }
                }
                // link-accounts/.../{success,cancel,fail: redirect from web AuthFlow completed to SDK
                else -> FinancialConnectionsSheetActivity::class.java
            }

            // native-redirect hosts:
            // redirections from embedded web AuthFlow back SDK (app2app start)
            host == "native-redirect" -> FinancialConnectionsSheetActivity::class.java

            else -> null
        }?.let { destinationActivity ->
                Intent(this@FinancialConnectionsSheetRedirectActivity,
                    destinationActivity
                )
            }
}

private fun Uri.isFinancialConnectionsScheme(): Boolean {
    return (this.scheme == "stripe-auth" || this.scheme == "stripe")
}
