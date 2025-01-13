package com.stripe.android.common.ui

import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract

internal class PaymentElementActivityResultCaller(
    private val key: String,
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
        return "${key}_${contract::class.java.name}"
    }
}
