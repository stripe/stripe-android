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
            isFinancialConnectionsUrl().not() -> null
            else -> {
                when {
                    path?.contains(PATH_CANCEL) == true ||
                        path?.contains(PATH_SUCCESS) == true -> Intent(
                        this@FinancialConnectionsSheetRedirectActivity,
                        FinancialConnectionsSheetActivity::class.java
                    )
                    // TODO@carlosmuvi - temporary deep link for native.
                    path?.contains(PATH_LOGIN) == true -> Intent(
                        this@FinancialConnectionsSheetRedirectActivity,
                        FinancialConnectionsSheetNativeActivity::class.java
                    )
                    else -> null
                }
            }
        }

    private fun Uri.isFinancialConnectionsUrl(): Boolean {
        return this.scheme == SCHEME && this.host == HOST
    }

    private companion object {
        private const val SCHEME = "stripe-auth"
        private const val HOST = "link-accounts"

        private const val PATH_CANCEL = "cancel"
        private const val PATH_SUCCESS = "success"
        private const val PATH_LOGIN = "login"
    }
}
