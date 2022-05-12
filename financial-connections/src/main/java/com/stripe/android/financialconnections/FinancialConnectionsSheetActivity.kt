package com.stripe.android.financialconnections

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.mvrx.asMavericksArgs
import com.airbnb.mvrx.viewModel
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Canceled

internal class FinancialConnectionsSheetActivity :
    AppCompatActivity(R.layout.activity_financialconnections_sheet) {

    val viewModel: FinancialConnectionsSheetViewModel by viewModel()

    private val starterArgs: FinancialConnectionsSheetActivityArgs? by lazy {
        FinancialConnectionsSheetActivityArgs.fromIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val fragment = FinancialConnectionsSheetFragment().apply {
                arguments = requireNotNull(starterArgs).asMavericksArgs()
            }
            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.nav_host_fragment, fragment, null)
                .commit()
        }
    }

    /**
     * Handles new intents in the form of the redirect from the custom tab hosted auth flow
     */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        viewModel.handleOnNewIntent(intent)
    }
}
