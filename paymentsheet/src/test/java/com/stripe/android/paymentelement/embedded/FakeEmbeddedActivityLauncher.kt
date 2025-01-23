package com.stripe.android.paymentelement.embedded

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine

internal class FakeEmbeddedActivityLauncher<T>(
    private val activityContract: ActivityResultContract<T, *>
) : ActivityResultLauncher<T>() {
    private val _argsTurbine = Turbine<T>()
    val argsTurbine: ReceiveTurbine<T> = _argsTurbine
    private val _unregisterTurbine = Turbine<Unit>()
    val unregisterTurbine: ReceiveTurbine<Unit> = _unregisterTurbine

    override fun launch(input: T, options: ActivityOptionsCompat?) {
        _argsTurbine.add(input)
    }

    override fun unregister() {
        _unregisterTurbine.add(Unit)
    }

    override fun getContract(): ActivityResultContract<T, *> {
        return activityContract
    }

    fun validate() {
        argsTurbine.ensureAllEventsConsumed()
        unregisterTurbine.ensureAllEventsConsumed()
    }
}
