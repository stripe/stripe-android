package com.stripe.android.utils

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import app.cash.turbine.Turbine
import com.stripe.android.challenge.passive.warmer.PassiveChallengeWarmer
import com.stripe.android.model.PassiveCaptchaParams

internal class FakePassiveChallengeWarmer : PassiveChallengeWarmer {
    data class RegisterCall(
        val activityResultCaller: ActivityResultCaller,
        val lifecycleOwner: LifecycleOwner,
    )

    val registerCalls = Turbine<RegisterCall>()
    val startCalls = Turbine<PassiveCaptchaParams>()
    val unregisterCalls = Turbine<Unit>()

    override fun register(activityResultCaller: ActivityResultCaller, lifecycleOwner: LifecycleOwner) {
        registerCalls.add(RegisterCall(activityResultCaller, lifecycleOwner))
    }

    override fun start(passiveCaptchaParams: PassiveCaptchaParams) {
        startCalls.add(passiveCaptchaParams)
    }

    override fun unregister() {
        unregisterCalls.add(Unit)
    }

    fun ensureAllEventsConsumed() {
        registerCalls.ensureAllEventsConsumed()
        startCalls.ensureAllEventsConsumed()
        unregisterCalls.ensureAllEventsConsumed()
    }
}
