package com.stripe.android.challenge.confirmation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

        listenForActivityResult()

        setContent {
            val bridgeReady by viewModel.bridgeReady.collectAsState(initial = false)
            IntentConfirmationChallengeUI(
                bridgeHandler = viewModel.bridgeHandler,
                bridgeReady = bridgeReady
            )
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
