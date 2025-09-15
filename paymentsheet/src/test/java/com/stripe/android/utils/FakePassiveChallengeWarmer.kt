package com.stripe.android.utils

import androidx.activity.result.ActivityResultCaller
import app.cash.turbine.Turbine
import com.stripe.android.challenge.warmer.PassiveChallengeWarmer
import com.stripe.android.model.PassiveCaptchaParams

class FakePassiveChallengeWarmer : PassiveChallengeWarmer {
    data class RegisterCall(
        val activityResultCaller: ActivityResultCaller,
    )

    data class StartCall(
        val passiveCaptchaParams: PassiveCaptchaParams,
        val publishableKey: String,
        val productUsage: Set<String>
    )

    private val registerCalls = Turbine<RegisterCall>()
    private val startCalls = Turbine<StartCall>()
    private val unregisterCalls = Turbine<Unit>()

    override fun register(activityResultCaller: ActivityResultCaller) {
        registerCalls.add(RegisterCall(activityResultCaller))
    }

    override fun start(
        passiveCaptchaParams: PassiveCaptchaParams,
        publishableKey: String,
        productUsage: Set<String>
    ) {
        startCalls.add(StartCall(passiveCaptchaParams, publishableKey, productUsage))
    }

    override fun unregister() {
        unregisterCalls.add(Unit)
    }

    suspend fun awaitRegisterCall(): RegisterCall = registerCalls.awaitItem()
    suspend fun awaitStartCall(): StartCall = startCalls.awaitItem()
    suspend fun awaitUnregisterCall(): Unit = unregisterCalls.awaitItem()

    fun ensureAllEventsConsumed() {
        registerCalls.ensureAllEventsConsumed()
        startCalls.ensureAllEventsConsumed()
        unregisterCalls.ensureAllEventsConsumed()
    }
}
