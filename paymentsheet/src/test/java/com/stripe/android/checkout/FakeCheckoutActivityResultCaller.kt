package com.stripe.android.checkout

import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat
import app.cash.turbine.Turbine

internal class FakeCheckoutActivityResultCaller(
    private val delegate: ActivityResultCaller,
) : ActivityResultCaller {
    var launchError: Throwable? = null
    val failedLaunches = Turbine<Any?>()

    override fun <I, O> registerForActivityResult(
        contract: ActivityResultContract<I, O>,
        callback: ActivityResultCallback<O>,
    ): ActivityResultLauncher<I> = wrap(delegate.registerForActivityResult(contract, callback))

    override fun <I, O> registerForActivityResult(
        contract: ActivityResultContract<I, O>,
        registry: ActivityResultRegistry,
        callback: ActivityResultCallback<O>,
    ): ActivityResultLauncher<I> = wrap(delegate.registerForActivityResult(contract, registry, callback))

    private fun <I> wrap(launcher: ActivityResultLauncher<I>): ActivityResultLauncher<I> {
        return object : ActivityResultLauncher<I>() {
            override fun launch(input: I, options: ActivityOptionsCompat?) {
                launchError?.let {
                    failedLaunches.add(input)
                    throw it
                }
                launcher.launch(input, options)
            }

            override fun unregister() = launcher.unregister()

            override val contract: ActivityResultContract<I, *>
                get() = launcher.contract
        }
    }

    fun ensureAllEventsConsumed() {
        failedLaunches.ensureAllEventsConsumed()
    }
}
