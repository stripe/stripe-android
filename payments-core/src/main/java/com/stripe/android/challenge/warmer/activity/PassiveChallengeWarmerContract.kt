package com.stripe.android.challenge.warmer.activity

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

    override fun parseResult(resultCode: Int, intent: Intent?) = PassiveChallengeWarmerResult

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Args(
        val passiveCaptchaParams: PassiveCaptchaParams,
        val publishableKey: String,
        val productUsage: Set<String>
    )

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        const val EXTRA_RESULT = "com.stripe.android.challenge.warmer.activity.PassiveChallengeWarmerContract.extra_result"
    }
}
