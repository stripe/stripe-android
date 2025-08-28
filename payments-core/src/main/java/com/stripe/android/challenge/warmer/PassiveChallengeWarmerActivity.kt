package com.stripe.android.challenge.warmer

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

internal class PassiveChallengeWarmerActivity : AppCompatActivity() {
    @VisibleForTesting
    internal var viewModelFactory: ViewModelProvider.Factory = PassiveChallengeWarmerViewModel.Factory

    private val viewModel by viewModels<PassiveChallengeWarmerViewModel> {
        viewModelFactory
    }

    override fun onStart() {
        super.onStart()

        lifecycleScope.launch {
            runCatching {
                viewModel.warmUp(this@PassiveChallengeWarmerActivity)
            }
            setResult(Activity.RESULT_OK, Intent())
            finish()
        }
    }

    companion object {
        internal const val EXTRA_ARGS = "passive_challenge_warmer_args"

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
