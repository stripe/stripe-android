package com.stripe.android.challenge.passive.warmer

import androidx.activity.result.ActivityResultCaller
import androidx.annotation.RestrictTo
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.model.PassiveCaptchaParams

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface PassiveChallengeWarmer {
    /**
     * Registers the host Activity and the lifecycle that owns warm-up work.
     */
    fun register(activityResultCaller: ActivityResultCaller, lifecycleOwner: LifecycleOwner)

    /**
     * Warms captcha on the registered host. Does nothing without a supported host.
     */
    fun start(passiveCaptchaParams: PassiveCaptchaParams)

    /**
     * Cancels warm-up work and releases the registered host.
     */
    fun unregister()
}
