package com.stripe.android.challenge.warmer

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.RestrictTo
import com.stripe.android.model.PassiveCaptchaParams

internal class PassiveChallengeWarmerActivityContract :
    ActivityResultContract<PassiveChallengeWarmerActivityContract.Args, Unit>() {

    override fun createIntent(context: Context, input: Args): Intent {
        val args = PassiveChallengeWarmerArgs(input.passiveCaptchaParams)
        return PassiveChallengeWarmerActivity.createIntent(context, args)
    }

    override fun parseResult(resultCode: Int, intent: Intent?) = Unit

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Args(
        val passiveCaptchaParams: PassiveCaptchaParams
    )
}
