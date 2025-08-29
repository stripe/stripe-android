package com.stripe.android.challenge.warmer

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.model.PassiveCaptchaParams

internal class DefaultPassiveChallengeWarmer : PassiveChallengeWarmer {
    private var launcher: ActivityResultLauncher<PassiveChallengeWarmerActivityContract.Args>? = null

    override fun register(activityResultCaller: ActivityResultCaller, lifecycleOwner: LifecycleOwner) {
        val contract = PassiveChallengeWarmerActivityContract()
        launcher = activityResultCaller.registerForActivityResult(contract) {}
        lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                super.onDestroy(owner)
                launcher?.unregister()
                launcher = null
            }
        })
    }

    override fun start(passiveCaptchaParams: PassiveCaptchaParams) {
        launcher?.launch(PassiveChallengeWarmerActivityContract.Args(passiveCaptchaParams))
    }
}
