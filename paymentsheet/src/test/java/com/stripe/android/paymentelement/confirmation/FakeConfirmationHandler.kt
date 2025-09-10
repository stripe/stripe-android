package com.stripe.android.paymentelement.confirmation

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import app.cash.turbine.Turbine
import com.stripe.android.model.PassiveCaptchaParams
import kotlinx.coroutines.flow.Flow
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
        lifecycleOwner: LifecycleOwner,
        passiveCaptchaParamsFlow: Flow<PassiveCaptchaParams?>
    ) {
        registerTurbine.add(
            RegisterCall(
                activityResultCaller = activityResultCaller,
                lifecycleOwner = lifecycleOwner,
                passiveCaptchaParamsFlow = passiveCaptchaParamsFlow
            )
        )
    }

    override suspend fun start(arguments: ConfirmationHandler.Args) {
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
        val passiveCaptchaParamsFlow: Flow<PassiveCaptchaParams?>
    )

    class Scenario(
        val handler: ConfirmationHandler,
        val confirmationState: MutableStateFlow<ConfirmationHandler.State>,
        val registerTurbine: Turbine<RegisterCall>,
        val startTurbine: Turbine<ConfirmationHandler.Args>,
        val awaitResultTurbine: Turbine<ConfirmationHandler.Result?>,
    )

    companion object {
        suspend fun test(
            hasReloadedFromProcessDeath: Boolean = false,
            initialState: ConfirmationHandler.State,
            block: suspend Scenario.() -> Unit,
        ) {
            val state = MutableStateFlow(initialState)

            val handler = FakeConfirmationHandler(
                hasReloadedFromProcessDeath = hasReloadedFromProcessDeath,
                state = state,
            )

            block(
                Scenario(
                    handler = handler,
                    confirmationState = state,
                    registerTurbine = handler.registerTurbine,
                    startTurbine = handler.startTurbine,
                    awaitResultTurbine = handler.awaitResultTurbine,
                )
            )

            handler.validate()
        }
    }
}
