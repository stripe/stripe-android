package com.stripe.android.utils

import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine

class DummyActivityResultCaller(
    private val onLaunch: () -> Unit = { error("Not implemented") }
) : ActivityResultCaller {
    private val _calls = Turbine<Call<*, *>>()
    val calls: ReceiveTurbine<Call<*, *>> = _calls

    override fun <I : Any?, O : Any?> registerForActivityResult(
        contract: ActivityResultContract<I, O>,
        callback: ActivityResultCallback<O>
    ): ActivityResultLauncher<I> {
        _calls.add(Call(contract, callback))

        return object : ActivityResultLauncher<I>() {
            override fun launch(input: I, options: ActivityOptionsCompat?) {
                onLaunch()
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
                onLaunch()
            }

            override fun unregister() {
                error("Not implemented")
            }

            override fun getContract(): ActivityResultContract<I, *> {
                error("Not implemented")
            }
        }
    }

    data class Call<I : Any?, O : Any?>(
        val contract: ActivityResultContract<I, O>,
        val callback: ActivityResultCallback<O>,
    )
}
