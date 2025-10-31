package com.stripe.android.challenge.confirmation

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.RestrictTo
import androidx.core.os.BundleCompat
import com.stripe.android.model.StripeIntent

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class IntentConfirmationChallengeActivityContract :
    ActivityResultContract<
        IntentConfirmationChallengeActivityContract.Args,
        IntentConfirmationChallengeActivityResult
        >() {

    override fun createIntent(context: Context, input: Args): Intent {
        return IntentConfirmationChallengeActivity.createIntent(
            context,
            IntentConfirmationChallengeArgs(
                input.publishableKey,
                input.productUsage.toList(),
                input.intent
            )
        )
    }

    override fun parseResult(resultCode: Int, intent: Intent?): IntentConfirmationChallengeActivityResult {
        val result = intent?.extras?.let {
            BundleCompat.getParcelable(it, EXTRA_RESULT, IntentConfirmationChallengeActivityResult::class.java)
        }
        return result ?: IntentConfirmationChallengeActivityResult.Failed(Throwable("No result"))
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Args(
        val publishableKey: String,
        val productUsage: Set<String>,
        val intent: StripeIntent
    )

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        const val EXTRA_RESULT =
            "com.stripe.android.challenge.confirmation.IntentConfirmationChallengeActivityContract.extra_result"
    }
}
