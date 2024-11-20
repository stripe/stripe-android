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
    private val registerCalls = Turbine<RegisterCall<*, *>>()
    private val launchCalls = Turbine<Any?>()

    override fun <I : Any?, O : Any?> registerForActivityResult(
        contract: ActivityResultContract<I, O>,
        callback: ActivityResultCallback<O>
    ): ActivityResultLauncher<I> {
        registerCalls.add(RegisterCall(contract, callback))

        return object : ActivityResultLauncher<I>() {
            override fun launch(input: I, options: ActivityOptionsCompat?) {
                launchCalls.add(input)
            }

            override fun unregister() {
                error("Not implemented")
            }

            override fun getContract(): ActivityResultContract<I, *> {
                error("Not implemented")
            }
        }
    }

    override fun <I : Any?, O : Any?> registerForActivityResult(
        contract: ActivityResultContract<I, O>,
        registry: ActivityResultRegistry,
        callback: ActivityResultCallback<O>
    ): ActivityResultLauncher<I> {
        return object : ActivityResultLauncher<I>() {
            override fun launch(input: I, options: ActivityOptionsCompat?) {
                launchCalls.add(input)
            }

            override fun unregister() {
                error("Not implemented")
            }

            override fun getContract(): ActivityResultContract<I, *> {
                error("Not implemented")
            }
        }
    }

    data class RegisterCall<I : Any?, O : Any?>(
        val contract: ActivityResultContract<I, O>,
        val callback: ActivityResultCallback<O>,
    )

    class Scenario(
        val activityResultCaller: ActivityResultCaller,
        private val registerCalls: ReceiveTurbine<RegisterCall<*, *>>,
        private val launchCalls: ReceiveTurbine<Any?>,
    ) {
        suspend fun awaitRegisterCall(): RegisterCall<*, *> {
            return registerCalls.awaitItem()
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
            ).apply {
                block(this)
                activityResultCaller.registerCalls.ensureAllEventsConsumed()
                activityResultCaller.launchCalls.ensureAllEventsConsumed()
            }
        }

        fun noOp(): ActivityResultCaller {
            return DummyActivityResultCaller()
        }
    }
}
