package com.stripe.android.paymentelement.confirmation

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import app.cash.turbine.Turbine
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeConfirmationHandler(
    override val hasReloadedFromProcessDeath: Boolean = false,
    override val state: MutableStateFlow<ConfirmationHandler.State> = MutableStateFlow(ConfirmationHandler.State.Idle)
) : ConfirmationHandler {
    val registerTurbine: Turbine<RegisterCall> = Turbine()
    val startTurbine: Turbine<ConfirmationHandler.Args> = Turbine()
    val awaitResultTurbine: Turbine<ConfirmationHandler.Result?> = Turbine(null)

    override fun register(
        activityResultCaller: ActivityResultCaller,
        lifecycleOwner: LifecycleOwner
    ) {
        registerTurbine.add(
            RegisterCall(
                activityResultCaller = activityResultCaller,
                lifecycleOwner = lifecycleOwner,
            )
        )
    }

    override fun start(arguments: ConfirmationHandler.Args) {
        startTurbine.add(arguments)
    }

    override suspend fun awaitResult(): ConfirmationHandler.Result? {
        return awaitResultTurbine.awaitItem()
    }

    fun validate() {
        registerTurbine.ensureAllEventsConsumed()
        startTurbine.ensureAllEventsConsumed()
        awaitResultTurbine.ensureAllEventsConsumed()
    }

    data class RegisterCall(
        val activityResultCaller: ActivityResultCaller,
        val lifecycleOwner: LifecycleOwner,
    )
}
