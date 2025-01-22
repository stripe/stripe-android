package com.stripe.android.paymentelement.embedded

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine

internal class FakeEmbeddedActivityLauncher<T>(
    private val activityContract: ActivityResultContract<T, *>
) : ActivityResultLauncher<T>() {
    private val _launchStateTurbine = Turbine<LaunchState<T>>()
    val launchTurbine: ReceiveTurbine<LaunchState<T>> = _launchStateTurbine
    private val _unregisterTurbine = Turbine<Boolean>()
    val unregisterTurbine: ReceiveTurbine<Boolean> = _unregisterTurbine

    override fun launch(input: T, options: ActivityOptionsCompat?) {
        _launchStateTurbine.add(
            LaunchState(
                didLaunch = true,
                launchArgs = input
            )
        )
    }

    override fun unregister() {
        _unregisterTurbine.add(true)
    }

    override fun getContract(): ActivityResultContract<T, *> {
        return activityContract
    }

    fun validate() {
        launchTurbine.ensureAllEventsConsumed()
        unregisterTurbine.ensureAllEventsConsumed()
    }

    data class LaunchState<T>(
        val didLaunch: Boolean,
        val launchArgs: T
    )
}
