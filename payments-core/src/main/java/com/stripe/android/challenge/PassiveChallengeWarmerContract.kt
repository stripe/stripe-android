package com.stripe.android.challenge

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.RestrictTo
import androidx.core.os.BundleCompat
import com.stripe.android.model.PassiveCaptchaParams

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PassiveChallengeWarmerContract :
    ActivityResultContract<PassiveChallengeWarmerContract.Args, PassiveChallengeWarmerResult>() {

    override fun createIntent(context: Context, input: Args): Intent {
        return PassiveChallengeWarmerActivity.createIntent(
            context,
            PassiveChallengeWarmerArgs(input.passiveCaptchaParams, input.publishableKey, input.productUsage.toList())
        )
    }

    override fun parseResult(resultCode: Int, intent: Intent?): PassiveChallengeWarmerResult {
        val result = intent?.extras?.let {
            BundleCompat.getParcelable(it, EXTRA_RESULT, PassiveChallengeWarmerResult::class.java)
        }
        return result ?: PassiveChallengeWarmerResult.Failed(Throwable("No result"))
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Args(
        val passiveCaptchaParams: PassiveCaptchaParams,
        val publishableKey: String,
        val productUsage: Set<String>
    )

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        const val EXTRA_RESULT = "com.stripe.android.challenge.PassiveChallengeWarmerContract.extra_result"
    }
}
