package com.stripe.android.challenge.warmer

import androidx.activity.result.ActivityResultCaller
import androidx.annotation.RestrictTo
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.model.PassiveCaptchaParams

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface PassiveChallengeWarmer {
    /**
     * Registers the passive challenge warmer with the given lifecycle owner and activity result caller.
     * This sets up the necessary activity result launcher and lifecycle observers for handling
     * passive challenge warming activities.
     *
     * @param activityResultCaller a caller class that can start confirmation activity flows
     * @param lifecycleOwner The owner of an observable lifecycle to attach the handlers to
     */
    fun register(
        activityResultCaller: ActivityResultCaller,
        lifecycleOwner: LifecycleOwner
    )

    /**
     * Starts the passive challenge warming process with the given captcha parameters.
     * This method should be called after [register] has been invoked to set up the necessary
     * activity result launcher.
     *
     * @param passiveCaptchaParams The parameters for the passive captcha challenge
     */
    fun start(passiveCaptchaParams: PassiveCaptchaParams)
}
