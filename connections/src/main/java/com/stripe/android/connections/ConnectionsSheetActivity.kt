package com.stripe.android.connections

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.stripe.android.connections.ConnectionsSheetViewEffect.FinishWithResult
import com.stripe.android.connections.ConnectionsSheetViewEffect.OpenAuthFlowWithUrl
import com.stripe.android.connections.databinding.ActivityConnectionsSheetBinding
import java.security.InvalidParameterException

internal class ConnectionsSheetActivity : AppCompatActivity() {

    private val startForResult = registerForActivityResult(StartActivityForResult()) {
        viewModel.onActivityResult()
    }

    @VisibleForTesting
    internal val viewBinding by lazy {
        ActivityConnectionsSheetBinding.inflate(layoutInflater)
    }

    @VisibleForTesting
    internal var viewModelFactory: ViewModelProvider.Factory =
        ConnectionsSheetViewModel.Factory(
            { application },
            { requireNotNull(starterArgs) },
            this,
            intent?.extras
        )

    private val viewModel: ConnectionsSheetViewModel by viewModels { viewModelFactory }

    private val starterArgs: ConnectionsSheetContract.Args? by lazy {
        ConnectionsSheetContract.Args.fromIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        val starterArgs = this.starterArgs
        if (starterArgs == null) {
            finishWithResult(
                ConnectionsSheetContract.Result.Failed(
                    IllegalArgumentException("ConnectionsSheet started without arguments.")
                )
            )
            return
        } else {
            try {
                starterArgs.validate()
            } catch (e: InvalidParameterException) {
                finishWithResult(ConnectionsSheetContract.Result.Failed(e))
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
        startForResult.launch(
            CustomTabsIntent.Builder()
                .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
                .build()
                .also { it.intent.data = Uri.parse(this.url) }
                .intent
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
     * If the back button is pressed during the manifest fetch or link account session fetch
     * return canceled result
     */
    override fun onBackPressed() {
        finishWithResult(ConnectionsSheetContract.Result.Canceled)
    }

    private fun finishWithResult(result: ConnectionsSheetContract.Result) {
        setResult(
            Activity.RESULT_OK,
            Intent().putExtras(result.toBundle())
        )
        finish()
    }
}
