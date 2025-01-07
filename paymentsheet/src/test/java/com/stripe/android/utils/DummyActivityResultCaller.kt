package com.stripe.android.utils

import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine

class DummyActivityResultCaller private constructor() : ActivityResultCaller {
    private val registeredLaunchers = Turbine<ActivityResultLauncher<*>>()
    private val registerCalls = Turbine<RegisterCall<*, *>>()
    private val launchCalls = Turbine<Any?>()
    private val unregisterCalls = Turbine<Unit>()

    override fun <I : Any?, O : Any?> registerForActivityResult(
        contract: ActivityResultContract<I, O>,
        callback: ActivityResultCallback<O>
    ): ActivityResultLauncher<I> {
        registerCalls.add(RegisterCall(contract, callback))

        val launcher = object : ActivityResultLauncher<I>() {
            override fun launch(input: I, options: ActivityOptionsCompat?) {
                launchCalls.add(input)
            }

            override fun unregister() {
                unregisterCalls.add(Unit)
            }

            override fun getContract(): ActivityResultContract<I, *> {
                error("Not implemented")
            }
        }

        registeredLaunchers.add(launcher)

        return launcher
    }

    override fun <I : Any?, O : Any?> registerForActivityResult(
        contract: ActivityResultContract<I, O>,
        registry: ActivityResultRegistry,
        callback: ActivityResultCallback<O>
    ): ActivityResultLauncher<I> {
        val launcher = object : ActivityResultLauncher<I>() {
            override fun launch(input: I, options: ActivityOptionsCompat?) {
                launchCalls.add(input)
            }

            override fun unregister() {
                unregisterCalls.add(Unit)
            }

            override fun getContract(): ActivityResultContract<I, *> {
                error("Not implemented")
            }
        }

        registeredLaunchers.add(launcher)

        return launcher
    }

    data class RegisterCall<I : Any?, O : Any?>(
        val contract: ActivityResultContract<I, O>,
        val callback: ActivityResultCallback<O>,
    )

    class Scenario(
        val activityResultCaller: ActivityResultCaller,
        private val registerCalls: ReceiveTurbine<RegisterCall<*, *>>,
        private val unregisterCalls: ReceiveTurbine<Unit>,
        private val launchCalls: ReceiveTurbine<Any?>,
        private val registeredLaunchers: ReceiveTurbine<ActivityResultLauncher<*>>,
    ) {
        suspend fun awaitNextRegisteredLauncher(): ActivityResultLauncher<*> {
            return registeredLaunchers.awaitItem()
        }

        suspend fun awaitRegisterCall(): RegisterCall<*, *> {
            return registerCalls.awaitItem()
        }

        suspend fun awaitUnregisterCall() {
            return unregisterCalls.awaitItem()
        }

        suspend fun awaitLaunchCall(): Any? {
            return launchCalls.awaitItem()
        }
    }

    companion object {
        suspend fun test(
            block: suspend Scenario.() -> Unit
        ) {
            val activityResultCaller = DummyActivityResultCaller()
            Scenario(
                activityResultCaller = activityResultCaller,
                registerCalls = activityResultCaller.registerCalls,
                launchCalls = activityResultCaller.launchCalls,
                registeredLaunchers = activityResultCaller.registeredLaunchers,
                unregisterCalls = activityResultCaller.unregisterCalls
            ).apply {
                block(this)
                activityResultCaller.registerCalls.ensureAllEventsConsumed()
                activityResultCaller.launchCalls.ensureAllEventsConsumed()
                activityResultCaller.registeredLaunchers.ensureAllEventsConsumed()
            }
        }

        fun noOp(): ActivityResultCaller {
            return DummyActivityResultCaller()
        }
    }
}
