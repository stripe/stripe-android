package com.stripe.android.challenge.confirmation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

internal class IntentConfirmationChallengeActivity : AppCompatActivity() {
    @VisibleForTesting
    internal var viewModelFactory: ViewModelProvider.Factory = IntentConfirmationChallengeViewModel.factory()

    private val viewModel: IntentConfirmationChallengeViewModel by viewModels {
        viewModelFactory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check if required args are present, finish gracefully if not
        if (!hasRequiredArgs()) {
            finish()
            return
        }

        listenForActivityResult()
        lifecycle.addObserver(viewModel)

        setContent {
            var showProgressIndicator by remember { mutableStateOf(true) }

            LaunchedEffect("BridgeEvents") {
                viewModel.bridgeReady.collect {
                    showProgressIndicator = false
                }
            }

            Scaffold(
                backgroundColor = Color.Transparent
            ) { innerPadding ->
                IntentConfirmationChallengeUI(
                    modifier = Modifier
                        .padding(innerPadding),
                    hostUrl = HOST_URL,
                    userAgent = viewModel.userAgent,
                    bridgeHandler = viewModel.bridgeHandler,
                    showProgressIndicator = showProgressIndicator,
                    closeClicked = viewModel::closeClicked,
                    webViewClientFactory = {
                        IntentConfirmationWebViewClient(
                            hostUrl = HOST_URL,
                            errorHandler = { error ->
                                viewModel.handleWebViewError(error)
                            },
                            openUri = { uri ->
                                val browserIntent = Intent(Intent.ACTION_VIEW, uri)
                                startActivity(browserIntent)
                            }
                        )
                    }
                )
            }
        }
    }

    private fun listenForActivityResult() {
        lifecycleScope.launch {
            viewModel.result.collect(::dismissWithResult)
        }
    }

    private fun dismissWithResult(result: IntentConfirmationChallengeActivityResult) {
        val bundle = bundleOf(
            IntentConfirmationChallengeActivityContract.EXTRA_RESULT to result
        )
        setResult(RESULT_COMPLETE, Intent().putExtras(bundle))
        finish()
    }

    private fun hasRequiredArgs(): Boolean {
        return intent?.extras?.containsKey(EXTRA_ARGS) == true
    }

    companion object {
        internal const val EXTRA_ARGS = "intent_confirmation_challenge_args"
        internal const val RESULT_COMPLETE = 4639
        internal const val HOST_URL = "https://b.stripecdn.com/mobile-confirmation-challenge/assets/index.html?v=1"

        internal fun createIntent(
            context: Context,
            args: IntentConfirmationChallengeArgs
        ): Intent {
            return Intent(context, IntentConfirmationChallengeActivity::class.java)
                .putExtras(getBundle(args))
        }

        internal fun getBundle(args: IntentConfirmationChallengeArgs): Bundle {
            return bundleOf(EXTRA_ARGS to args)
        }

        internal fun getArgs(savedStateHandle: SavedStateHandle): IntentConfirmationChallengeArgs? {
            return savedStateHandle.get<IntentConfirmationChallengeArgs>(EXTRA_ARGS)
        }
    }
}
