package com.stripe.android.utils

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import app.cash.turbine.Turbine
import com.stripe.android.challenge.warmer.PassiveChallengeWarmer
import com.stripe.android.model.PassiveCaptchaParams

class FakePassiveChallengeWarmer : PassiveChallengeWarmer {
    data class RegisterCall(
        val activityResultCaller: ActivityResultCaller,
        val lifecycleOwner: LifecycleOwner
    )

    data class StartCall(
        val passiveCaptchaParams: PassiveCaptchaParams,
        val publishableKey: String,
        val productUsage: Set<String>
    )

    private val registerCalls = Turbine<RegisterCall>()
    private val startCalls = Turbine<StartCall>()

    override fun register(activityResultCaller: ActivityResultCaller, lifecycleOwner: LifecycleOwner) {
        registerCalls.add(RegisterCall(activityResultCaller, lifecycleOwner))
    }

    override fun start(
        passiveCaptchaParams: PassiveCaptchaParams,
        publishableKey: String,
        productUsage: Set<String>
    ) {
        startCalls.add(StartCall(passiveCaptchaParams, publishableKey, productUsage))
    }

    suspend fun awaitRegisterCall(): RegisterCall = registerCalls.awaitItem()
    suspend fun awaitStartCall(): StartCall = startCalls.awaitItem()

    fun ensureAllEventsConsumed() {
        registerCalls.ensureAllEventsConsumed()
        startCalls.ensureAllEventsConsumed()
    }
}
