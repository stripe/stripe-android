package com.stripe.android.financialconnections

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * This Activity handles redirects from the ChromeCustomTab hosting the AuthFlow.
 * It'll process the result url in [Intent.getData] and pass them back to the opening activity,
 * [FinancialConnectionsSheetActivity].
 */
class FinancialConnectionsSheetRedirectActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /**
         * Used together, FLAG_ACTIVITY_SINGLE_TOP and FLAG_ACTIVITY_CLEAR_TOP
         * clear everything on the stack above the opening activity, including CCT.
         */
        Intent(this, FinancialConnectionsSheetActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .also { it.data = intent.data }
            .let { startActivity(it) }
        finish()
    }
}
