package com.stripe.android.paymentsheet.flowcontroller

import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract

internal class FlowControllerActivityResultCaller(
    private val registryOwner: ActivityResultRegistryOwner
) : ActivityResultCaller {
    override fun <I : Any?, O : Any?> registerForActivityResult(
        contract: ActivityResultContract<I, O>,
        callback: ActivityResultCallback<O>
    ): ActivityResultLauncher<I> {
        return registryOwner.activityResultRegistry.register(createKey(contract), contract, callback)
    }

    override fun <I : Any?, O : Any?> registerForActivityResult(
        contract: ActivityResultContract<I, O>,
        registry: ActivityResultRegistry,
        callback: ActivityResultCallback<O>
    ): ActivityResultLauncher<I> {
        return registry.register(createKey(contract), contract, callback)
    }

    private fun <I : Any?, O : Any?> createKey(contract: ActivityResultContract<I, O>): String {
        return "${FLOW_CONTROLLER_KEY}_${contract::class.java.name}"
    }

    private companion object {
        const val FLOW_CONTROLLER_KEY = "FlowController"
    }
}
