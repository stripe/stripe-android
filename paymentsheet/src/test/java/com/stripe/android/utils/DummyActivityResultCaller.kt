package com.stripe.android.utils

import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat

class DummyActivityResultCaller : ActivityResultCaller {

    override fun <I : Any?, O : Any?> registerForActivityResult(
        contract: ActivityResultContract<I, O>,
        callback: ActivityResultCallback<O>
    ): ActivityResultLauncher<I> {
        return object : ActivityResultLauncher<I>() {
            override fun launch(input: I, options: ActivityOptionsCompat?) {
                error("Not implemented")
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
                error("Not implemented")
            }

            override fun unregister() {
                error("Not implemented")
            }

            override fun getContract(): ActivityResultContract<I, *> {
                error("Not implemented")
            }
        }
    }
}
