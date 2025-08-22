package com.stripe.android.challenge.warmer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

internal class PassiveChallengeWarmerActivity : AppCompatActivity() {
    @VisibleForTesting
    internal var viewModelFactory: ViewModelProvider.Factory = PassiveChallengeWarmerViewModel.Factory

    private val viewModel by viewModels<PassiveChallengeWarmerViewModel> {
        viewModelFactory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            viewModel.warmUp(this@PassiveChallengeWarmerActivity)
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
    }
}
