package com.stripe.android.paymentelement

import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract

@Suppress("UNCHECKED_CAST")
internal class FakeActivityResultCaller(vararg launchers: ActivityResultLauncher<*>) : ActivityResultCaller {
    private val contractLauncherMap: Map<ActivityResultContract<*, *>, ActivityResultLauncher<*>> =
        launchers.associateBy { launcher ->
            launcher.contract
        }

    override fun <I : Any?, O : Any?> registerForActivityResult(
        contract: ActivityResultContract<I, O>,
        callback: ActivityResultCallback<O>
    ): ActivityResultLauncher<I> {
        return contractLauncherMap[contract] as ActivityResultLauncher<I>
    }

    override fun <I : Any?, O : Any?> registerForActivityResult(
        contract: ActivityResultContract<I, O>,
        registry: ActivityResultRegistry,
        callback: ActivityResultCallback<O>
    ): ActivityResultLauncher<I> {
        return contractLauncherMap[contract] as ActivityResultLauncher<I>
    }
}
