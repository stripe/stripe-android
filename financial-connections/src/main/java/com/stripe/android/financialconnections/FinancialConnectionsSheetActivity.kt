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
import androidx.lifecycle.lifecycleScope
import com.stripe.android.financialconnections.FinancialConnectionsSheetViewEffect.FinishWithResult
import com.stripe.android.financialconnections.FinancialConnectionsSheetViewEffect.OpenAuthFlowWithUrl
import com.stripe.android.financialconnections.databinding.ActivityFinancialconnectionsSheetBinding
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult
import com.stripe.android.financialconnections.presentation.CreateBrowserIntentForUrl
import java.security.InvalidParameterException

internal class FinancialConnectionsSheetActivity : AppCompatActivity() {

    private val startForResult = registerForActivityResult(StartActivityForResult()) {
        viewModel.onActivityResult()
    }

    @VisibleForTesting
    internal val viewBinding by lazy {
        ActivityFinancialconnectionsSheetBinding.inflate(layoutInflater)
    }

    @VisibleForTesting
    internal var viewModelFactory: ViewModelProvider.Factory =
        FinancialConnectionsSheetViewModel.Factory(
            { application },
            { requireNotNull(starterArgs) },
            this,
            intent?.extras
        )

    private val viewModel: FinancialConnectionsSheetViewModel by viewModels { viewModelFactory }

    private val starterArgs: FinancialConnectionsSheetActivityArgs? by lazy {
        FinancialConnectionsSheetActivityArgs.fromIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        val starterArgs = this.starterArgs
        if (starterArgs == null) {
            finishWithResult(
                FinancialConnectionsSheetActivityResult.Failed(
                    IllegalArgumentException("ConnectionsSheet started without arguments.")
                )
            )
            return
        } else {
            try {
                starterArgs.validate()
            } catch (e: InvalidParameterException) {
                finishWithResult(FinancialConnectionsSheetActivityResult.Failed(e))
                return
            }
        }

        setupObservers()
        if (savedInstanceState != null) viewModel.onActivityRecreated()
    }

    private fun setupObservers() {
        lifecycleScope.launchWhenStarted {
            viewModel.state.collect {
                // process state updates here.
            }
        }
        lifecycleScope.launchWhenStarted {
            viewModel.viewEffect.collect { viewEffect ->
                when (viewEffect) {
                    is OpenAuthFlowWithUrl -> viewEffect.launch()
                    is FinishWithResult -> finishWithResult(viewEffect.result)
                }
            }
        }
    }

    private fun OpenAuthFlowWithUrl.launch() {
        val uri = Uri.parse(this.url)
        startForResult.launch(
            CreateBrowserIntentForUrl(
                context = this@FinancialConnectionsSheetActivity,
                uri = uri,
            )
        )
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    /**
     * Handles new intents in the form of the redirect from the custom tab hosted auth flow
     */
    public override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        viewModel.handleOnNewIntent(intent)
    }

    /**
     * If the back button is pressed during the manifest fetch or session fetch
     * return canceled result
     */
    override fun onBackPressed() {
        finishWithResult(FinancialConnectionsSheetActivityResult.Canceled)
    }

    private fun finishWithResult(result: FinancialConnectionsSheetActivityResult) {
        setResult(
            Activity.RESULT_OK,
            Intent().putExtras(result.toBundle())
        )
        finish()
    }
}
