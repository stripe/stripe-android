package com.stripe.android.challenge

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

internal class PassiveChallengeActivity : AppCompatActivity() {
    @VisibleForTesting
    internal var viewModelFactory: ViewModelProvider.Factory = PassiveChallengeViewModel.factory()

    private val viewModel: PassiveChallengeViewModel by viewModels<PassiveChallengeViewModel> {
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
            viewModel.startPassiveChallenge(this@PassiveChallengeActivity)
        }
    }

    private fun dismissWithResult(result: PassiveChallengeActivityResult) {
        val bundle = bundleOf(
            PassiveChallengeActivityContract.EXTRA_RESULT to result
        )
        setResult(RESULT_COMPLETE, Intent().putExtras(bundle))
        finish()
    }

    companion object {
        internal const val EXTRA_ARGS = "passive_challenge_args"
        internal const val RESULT_COMPLETE = 4646

        internal fun createIntent(
            context: Context,
            args: PassiveChallengeArgs
        ): Intent {
            return Intent(context, PassiveChallengeActivity::class.java)
                .putExtra(EXTRA_ARGS, args)
        }

        internal fun getArgs(savedStateHandle: SavedStateHandle): PassiveChallengeArgs? {
            return savedStateHandle.get<PassiveChallengeArgs>(EXTRA_ARGS)
        }
    }
}
