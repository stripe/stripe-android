package com.stripe.android.paymentelement.embedded.manage

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine

internal class FakeManageActivityLauncher : ActivityResultLauncher<ManageContract.Args>() {
    private val _launchStateTurbine = Turbine<LaunchState>()
    val launchTurbine: ReceiveTurbine<LaunchState> = _launchStateTurbine
    private val _unregisterTurbine = Turbine<Boolean>()
    val unregisterTurbine: ReceiveTurbine<Boolean> = _unregisterTurbine

    override fun launch(input: ManageContract.Args?, options: ActivityOptionsCompat?) {
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

    override fun getContract(): ActivityResultContract<ManageContract.Args, ManageResult> {
        return ManageContract
    }

    fun validate() {
        launchTurbine.ensureAllEventsConsumed()
        unregisterTurbine.ensureAllEventsConsumed()
    }

    data class LaunchState(
        val didLaunch: Boolean,
        val launchArgs: ManageContract.Args?
    )
}
