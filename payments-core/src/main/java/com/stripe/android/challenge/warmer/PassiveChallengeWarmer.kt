package com.stripe.android.challenge.warmer

import androidx.activity.result.ActivityResultCaller
import androidx.annotation.RestrictTo
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.model.PassiveCaptchaParams

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface PassiveChallengeWarmer {
    /**
     * Registers all internal confirmation sub-handlers onto the given lifecycle owner.
     *
     * @param activityResultCaller a caller class that can start confirmation activity flows
     * @param lifecycleOwner The owner of an observable lifecycle to attach the handlers to
     */
    fun start(
        activityResultCaller: ActivityResultCaller,
        lifecycleOwner: LifecycleOwner,
        passiveCaptchaParams: PassiveCaptchaParams
    )
}
