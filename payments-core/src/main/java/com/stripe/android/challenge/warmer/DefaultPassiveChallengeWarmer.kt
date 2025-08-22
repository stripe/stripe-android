package com.stripe.android.challenge.warmer

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.model.PassiveCaptchaParams

internal class DefaultPassiveChallengeWarmer : PassiveChallengeWarmer {
    override fun start(
        activityResultCaller: ActivityResultCaller,
        lifecycleOwner: LifecycleOwner,
        passiveCaptchaParams: PassiveCaptchaParams
    ) {
        lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                super.onStart(owner)
                val contract = PassiveChallengeWarmerActivityContract()
                val launcher = activityResultCaller.registerForActivityResult(contract) {}
                launcher.launch(PassiveChallengeWarmerActivityContract.Args(passiveCaptchaParams))
            }
        })
    }
}
