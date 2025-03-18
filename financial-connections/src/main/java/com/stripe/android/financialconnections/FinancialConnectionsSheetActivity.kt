package com.stripe.android.financialconnections

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.financialconnections.FinancialConnectionsSheetViewEffect.FinishWithResult
import com.stripe.android.financialconnections.FinancialConnectionsSheetViewEffect.OpenAuthFlowWithUrl
import com.stripe.android.financialconnections.FinancialConnectionsSheetViewEffect.OpenNativeAuthFlow
import com.stripe.android.financialconnections.browser.BrowserManager
import com.stripe.android.financialconnections.features.common.LoadingSpinner
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs.Companion.EXTRA_ARGS
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetNativeActivityArgs
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsBottomSheetLayout
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.uicore.elements.bottomsheet.StripeBottomSheetState
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetState
import com.stripe.android.uicore.utils.collectAsState

internal class FinancialConnectionsSheetActivity : AppCompatActivity() {

    val viewModel: FinancialConnectionsSheetViewModel by viewModels(
        factoryProducer = { FinancialConnectionsSheetViewModel.Factory }
    )

    private val startBrowserForResult = registerForActivityResult(StartActivityForResult()) {
        viewModel.onBrowserActivityResult()
    }

    private val startNativeAuthFlowForResult = registerForActivityResult(StartActivityForResult()) {
        viewModel.onNativeAuthFlowResult(it)
    }

    private lateinit var browserManager: BrowserManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = getArgs(intent)
        if (args == null) {
            finish()
            return
        }

        browserManager = BrowserManager(application)

        if (savedInstanceState != null) {
            viewModel.onActivityRecreated()
        }

        setContent {
            val bottomSheetState = rememberStripeBottomSheetState()
            val state by viewModel.stateFlow.collectAsState()

            LaunchedEffect(state.viewEffect) {
                state.viewEffect?.let { viewEffect ->
                    handleViewEffect(
                        viewEffect = viewEffect,
                        bottomSheetState = bottomSheetState,
                    )
                    viewModel.onViewEffectLaunched()
                }
            }

            BackHandler {
                viewModel.onDismissed()
            }

            FinancialConnectionsTheme(state.theme) {
                FinancialConnectionsBottomSheetLayout(
                    state = bottomSheetState,
                    onDismissed = viewModel::onDismissed,
                ) {
                    Loading()
                }
            }
        }
    }

    @Composable
    private fun Loading() {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            LoadingSpinner(Modifier.size(52.dp))
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    /**
     * Handles new intents in the form of the redirect from the custom tab hosted auth flow
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        viewModel.handleOnNewIntent(intent)
    }

    private suspend fun handleViewEffect(
        viewEffect: FinancialConnectionsSheetViewEffect,
        bottomSheetState: StripeBottomSheetState,
    ) {
        when (viewEffect) {
            is OpenAuthFlowWithUrl -> {
                startBrowserForResult.launch(
                    browserManager.createBrowserIntentForUrl(
                        uri = Uri.parse(viewEffect.url)
                    )
                )
            }

            is FinishWithResult -> {
                viewEffect.finishToast?.let { resId ->
                    Toast.makeText(this, resId, Toast.LENGTH_LONG).show()
                }
                bottomSheetState.hide()
                finishWithResult(viewEffect.result)
            }

            is OpenNativeAuthFlow -> {
                openNativeAuthFlow(viewEffect)
            }
        }
    }

    private fun openNativeAuthFlow(viewEffect: OpenNativeAuthFlow) {
        startNativeAuthFlowForResult.launch(
            FinancialConnectionsSheetNativeActivity.intent(
                context = this,
                args = FinancialConnectionsSheetNativeActivityArgs(
                    initialSyncResponse = viewEffect.initialSyncResponse,
                    configuration = viewEffect.configuration,
                    elementsSessionContext = viewEffect.elementsSessionContext,
                )
            )
        )
    }

    private fun finishWithResult(result: FinancialConnectionsSheetActivityResult) {
        setResult(RESULT_OK, Intent().putExtras(result.toBundle()))
        finish()
    }

    companion object {

        fun intent(context: Context, args: FinancialConnectionsSheetActivityArgs): Intent {
            return Intent(context, FinancialConnectionsSheetActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                putExtra(EXTRA_ARGS, args)
            }
        }

        fun getArgs(savedStateHandle: SavedStateHandle): FinancialConnectionsSheetActivityArgs? {
            return savedStateHandle.get<FinancialConnectionsSheetActivityArgs>(EXTRA_ARGS)
        }

        fun getArgs(intent: Intent): FinancialConnectionsSheetActivityArgs? {
            return intent.getParcelableExtra(EXTRA_ARGS)
        }
    }
}
