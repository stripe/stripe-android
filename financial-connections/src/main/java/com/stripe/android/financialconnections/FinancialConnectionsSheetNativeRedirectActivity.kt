package com.stripe.android.financialconnections

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity

/**
 * This Activity handles redirects from the Institution authorization flow hand-off from native.
 * It'll process the result url in [Intent.getData] and pass them back to the opening activity,
 * [FinancialConnectionsSheetNativeActivity].
 */
class FinancialConnectionsSheetNativeRedirectActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /**
         * Used together, FLAG_ACTIVITY_SINGLE_TOP and FLAG_ACTIVITY_CLEAR_TOP
         * clear everything on the stack above the opening activity, including CCT.
         */
        Intent(this, FinancialConnectionsSheetNativeActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .also { it.data = intent.data }
            .let { startActivity(it) }
        finish()
    }
}
