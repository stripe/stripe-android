package com.stripe.android.challenge.warmer

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.challenge.warmer.activity.PassiveChallengeWarmerContract
import com.stripe.android.model.PassiveCaptchaParams

internal class DefaultPassiveChallengeWarmer : PassiveChallengeWarmer {
    private var launcher: ActivityResultLauncher<PassiveChallengeWarmerContract.Args>? = null

    override fun register(activityResultCaller: ActivityResultCaller) {
        val contract = PassiveChallengeWarmerContract()
        launcher?.unregister()
        launcher = activityResultCaller.registerForActivityResult(contract) {}
    }

    override fun start(
        passiveCaptchaParams: PassiveCaptchaParams,
        publishableKey: String,
        productUsage: Set<String>
    ) {
        launcher?.launch(
            PassiveChallengeWarmerContract.Args(
                passiveCaptchaParams = passiveCaptchaParams,
                publishableKey = publishableKey,
                productUsage = productUsage
            )
        )
    }

    override fun unregister() {
        launcher?.unregister()
        launcher = null
    }
}
