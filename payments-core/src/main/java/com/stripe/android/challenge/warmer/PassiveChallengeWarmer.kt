package com.stripe.android.challenge.warmer

import androidx.activity.result.ActivityResultCaller
import androidx.annotation.RestrictTo
import com.stripe.android.model.PassiveCaptchaParams

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface PassiveChallengeWarmer {
    /**
     * Registers the passive challenge warmer with the given activity result caller.
     * This sets up the necessary activity result launcher for handling
     * passive challenge warming activities.
     *
     * @param activityResultCaller a caller class that can start confirmation activity flows
     */
    fun register(activityResultCaller: ActivityResultCaller)

    /**
     * Starts the passive challenge warming process with the given captcha parameters.
     * This method should be called after [register] has been invoked to set up the necessary
     * activity result launcher.
     *
     * @param passiveCaptchaParams The parameters for the passive captcha challenge
     * @param publishableKey The publishable key for Stripe API
     * @param productUsage Set of product usage strings for analytics
     */
    fun start(
        passiveCaptchaParams: PassiveCaptchaParams,
        publishableKey: String,
        productUsage: Set<String>
    )

    /**
     * Unregisters the activity result launcher and clears any internal references.
     * This should be called to clean up resources when the warmer is no longer needed.
     */
    fun unregister()
}
