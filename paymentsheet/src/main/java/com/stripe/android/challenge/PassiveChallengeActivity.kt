package com.stripe.android.challenge

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle

internal class PassiveChallengeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dismissWithResult(
            result = PassiveChallengeActivityResult.Failed(
                error = NotImplementedError("Passive challenges not implemented yet")
            )
        )
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
        internal const val RESULT_COMPLETE = 4638

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
