package com.stripe.android.financialconnections

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.airbnb.mvrx.Mavericks
import com.stripe.android.financialconnections.FinancialConnectionsSheetViewEffect.FinishWithResult
import com.stripe.android.financialconnections.FinancialConnectionsSheetViewEffect.OpenAuthFlowWithUrl
import com.stripe.android.financialconnections.FinancialConnectionsSheetViewEffect.OpenNativeAuthFlow
import com.stripe.android.financialconnections.browser.BrowserManager
import com.stripe.android.financialconnections.features.common.LoadingSpinner
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetNativeActivityArgs
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.financialconnections.utils.argsOrNull
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal class FinancialConnectionsSheetActivity : AppCompatActivity() {

    val viewModel: FinancialConnectionsSheetViewModel by viewModels(
        factoryProducer = { FinancialConnectionsSheetViewModel.Factory }
    )

    val args by argsOrNull<FinancialConnectionsSheetActivityArgs>()

    private val startBrowserForResult = registerForActivityResult(StartActivityForResult()) {
        viewModel.onBrowserActivityResult()
    }

    private val startNativeAuthFlowForResult = registerForActivityResult(StartActivityForResult()) {
        viewModel.onNativeAuthFlowResult(it)
    }

    private lateinit var browserManager: BrowserManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (args == null) {
            finish()
        } else {
            observeViewEffects()
            browserManager = BrowserManager(application)
            if (savedInstanceState != null) viewModel.onActivityRecreated()
        }

        onBackPressedDispatcher.addCallback {
            finishWithResult(FinancialConnectionsSheetActivityResult.Canceled)
        }
        setContent { Loading() }
    }

    @Composable
    private fun Loading() {
        FinancialConnectionsTheme {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                LoadingSpinner(Modifier.size(52.dp))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    /**
     * Handles new intents in the form of the redirect from the custom tab hosted auth flow
     */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        viewModel.handleOnNewIntent(intent)
    }

    /**
     * handle state changes here.
     */

    private fun observeViewEffects() = lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.stateFlow
                .map { it.viewEffect }
                .distinctUntilChanged()
                .collect { viewEffect ->
                    if (viewEffect == null) return@collect
                    when (viewEffect) {
                        is OpenAuthFlowWithUrl -> startBrowserForResult.launch(
                            browserManager.createBrowserIntentForUrl(
                                uri = Uri.parse(viewEffect.url)
                            )
                        )

                        is FinishWithResult -> {
                            viewEffect.finishToast?.let { resId ->
                                Toast.makeText(
                                    this@FinancialConnectionsSheetActivity,
                                    resId,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            finishWithResult(viewEffect.result)
                        }

                        is OpenNativeAuthFlow -> openNativeAuthFlow(viewEffect)
                    }
                    viewModel.onViewEffectLaunched()
                }
        }
    }

    private fun openNativeAuthFlow(viewEffect: OpenNativeAuthFlow) {
        startNativeAuthFlowForResult.launch(
            Intent(
                this@FinancialConnectionsSheetActivity,
                FinancialConnectionsSheetNativeActivity::class.java
            ).also {
                it.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                it.putExtra(
                    Mavericks.KEY_ARG,
                    FinancialConnectionsSheetNativeActivityArgs(
                        initialSyncResponse = viewEffect.initialSyncResponse,
                        configuration = viewEffect.configuration
                    )
                )
            }
        )
    }

    private fun finishWithResult(result: FinancialConnectionsSheetActivityResult) {
        setResult(RESULT_OK, Intent().putExtras(result.toBundle()))
        finish()
    }
}
