package com.stripe.android.payments.core.authentication.challenge

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.RestrictTo
import androidx.core.os.BundleCompat
import com.stripe.android.model.StripeIntent

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class IntentConfirmationChallengeActivityContract :
    ActivityResultContract<IntentConfirmationChallengeActivityContract.Args, IntentConfirmationChallengeActivityResult>() {

    override fun createIntent(context: Context, input: Args): Intent {
        return IntentConfirmationChallengeActivity.createIntent(
            context,
            IntentConfirmationChallengeArgs(
                input.intentConfirmationChallenge,
                input.publishableKey,
                input.productUsage.toList()
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
        val intentConfirmationChallenge: StripeIntent.NextActionData.SdkData.IntentConfirmationChallenge,
        val publishableKey: String,
        val productUsage: Set<String>
    )

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        const val EXTRA_RESULT = "com.stripe.android.payments.core.authentication.challenge.IntentConfirmationChallengeActivityContract.extra_result"
    }
}
