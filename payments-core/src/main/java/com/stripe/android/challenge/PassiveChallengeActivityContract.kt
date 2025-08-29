package com.stripe.android.challenge

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.RestrictTo
import androidx.core.os.BundleCompat
import com.stripe.android.model.PassiveCaptchaParams

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PassiveChallengeActivityContract :
    ActivityResultContract<PassiveChallengeActivityContract.Args, PassiveChallengeActivityResult>() {

    override fun createIntent(context: Context, input: Args): Intent {
        return PassiveChallengeActivity.createIntent(
            context,
            PassiveChallengeArgs(input.passiveCaptchaParams, input.publishableKey, input.productUsage.toList())
        )
    }

    override fun parseResult(resultCode: Int, intent: Intent?): PassiveChallengeActivityResult {
        val result = intent?.extras?.let {
            BundleCompat.getParcelable(it, EXTRA_RESULT, PassiveChallengeActivityResult::class.java)
        }
        return result ?: PassiveChallengeActivityResult.Failed(Throwable("No result"))
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Args(
        val passiveCaptchaParams: PassiveCaptchaParams,
        val publishableKey: String,
        val productUsage: Set<String>
    )

    internal companion object {
        const val EXTRA_RESULT = "com.stripe.android.challenge.PassiveChallengeActivityContract.extra_result"
    }
}
