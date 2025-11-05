package com.stripe.android.challenge.confirmation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.coroutines.launch

internal class IntentConfirmationChallengeActivity : AppCompatActivity() {
    @VisibleForTesting
    internal var viewModelFactory: ViewModelProvider.Factory = IntentConfirmationChallengeViewModel.factory()

    private val viewModel: IntentConfirmationChallengeViewModel by viewModels {
        viewModelFactory
    }

    private val webView: IntentConfirmationChallengeWebView by lazy {
        IntentConfirmationChallengeWebView(this, viewModel.bridgeHandler)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        listenForActivityResult()

        setContent {
            val showWebView by viewModel.showWebView.collectAsState()
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                if (showWebView.not()) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                    )
                }

                AnimatedVisibility(
                    visible = showWebView,
                    modifier = Modifier
                        .fillMaxSize(),
                ) {
                    AndroidView(
                        modifier = Modifier
                            .fillMaxSize(),
                        factory = {
                            webView
                        },
                        update = { view ->
                            view.layoutParams =
                                ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                        }
                    )
                }
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

    companion object {
        internal const val EXTRA_ARGS = "intent_confirmation_challenge_args"
        internal const val RESULT_COMPLETE = 4639

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
