package com.stripe.android.payments.core.authentication.challenge

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

internal class IntentConfirmationChallengeActivity : AppCompatActivity() {
    @VisibleForTesting
    internal var viewModelFactory: ViewModelProvider.Factory = IntentConfirmationChallengeViewModel.Factory

    private val viewModel: IntentConfirmationChallengeViewModel by viewModels<IntentConfirmationChallengeViewModel> {
        viewModelFactory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            viewModel.result.collect { result ->
                dismissWithResult(result)
            }
        }

        lifecycleScope.launch {
            viewModel.startIntentConfirmationChallenge(this@IntentConfirmationChallengeActivity)
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
                .putExtra(EXTRA_ARGS, args)
        }

        internal fun getArgs(savedStateHandle: SavedStateHandle): IntentConfirmationChallengeArgs? {
            return savedStateHandle.get<IntentConfirmationChallengeArgs>(EXTRA_ARGS)
        }
    }
}
