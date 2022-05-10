package com.stripe.android.financialconnections

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.InternalMavericksApi
import com.airbnb.mvrx.Mavericks
import com.airbnb.mvrx.MavericksView
import com.airbnb.mvrx.MavericksViewModelProvider
import com.airbnb.mvrx.asMavericksArgs
import com.airbnb.mvrx.viewModel
import com.stripe.android.financialconnections.FinancialConnectionsSheetViewEffect.OpenAuthFlowWithUrl
import com.stripe.android.financialconnections.databinding.ActivityFinancialconnectionsSheetBinding
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Canceled
import com.stripe.android.financialconnections.presentation.CreateBrowserIntentForUrl
import java.security.InvalidParameterException

internal class FinancialConnectionsSheetActivity : AppCompatActivity() {

    @VisibleForTesting
    internal val viewBinding by lazy {
        ActivityFinancialconnectionsSheetBinding.inflate(layoutInflater)
    }

     val viewModel: FinancialConnectionsSheetViewModel by viewModel()

    private val starterArgs: FinancialConnectionsSheetActivityArgs? by lazy {
        FinancialConnectionsSheetActivityArgs.fromIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        if (savedInstanceState == null) addFragment()
    }

    private fun addFragment() {
        val fragment = FinancialConnectionsSheetFragment()
        fragment.arguments = requireNotNull(starterArgs).asMavericksArgs()
        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .add(R.id.nav_host_fragment, fragment, null)
            .commit()
    }

    /**
     * Handles new intents in the form of the redirect from the custom tab hosted auth flow
     */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        viewModel.handleOnNewIntent(intent)
    }

    /**
     * If the back button is pressed during the manifest fetch or session fetch
     * return canceled result
     */
    override fun onBackPressed() {
        setResult(Activity.RESULT_OK, Intent().putExtras(Canceled.toBundle()))
        finish()
    }
}
