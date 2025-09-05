package com.stripe.android.challenge.warmer.activity

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

internal class PassiveChallengeWarmerActivity : AppCompatActivity() {
    @VisibleForTesting
    internal var viewModelFactory: ViewModelProvider.Factory = PassiveChallengeWarmerViewModel.Factory

    private val viewModel: PassiveChallengeWarmerViewModel by viewModels<PassiveChallengeWarmerViewModel> {
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
            viewModel.warmUpPassiveChallenge(this@PassiveChallengeWarmerActivity)
        }
    }

    private fun dismissWithResult(result: PassiveChallengeWarmerResult) {
        val bundle = bundleOf(
            PassiveChallengeWarmerContract.EXTRA_RESULT to result
        )
        setResult(RESULT_COMPLETE, Intent().putExtras(bundle))
        finish()
    }

    companion object {
        internal const val EXTRA_ARGS = "passive_challenge_warmer_args"
        internal const val RESULT_COMPLETE = 4639

        internal fun createIntent(
            context: Context,
            args: PassiveChallengeWarmerArgs
        ): Intent {
            return Intent(context, PassiveChallengeWarmerActivity::class.java)
                .putExtra(EXTRA_ARGS, args)
        }

        internal fun getArgs(savedStateHandle: SavedStateHandle): PassiveChallengeWarmerArgs? {
            return savedStateHandle.get<PassiveChallengeWarmerArgs>(EXTRA_ARGS)
        }
    }
}
