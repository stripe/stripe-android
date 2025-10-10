package com.stripe.android.paymentsheet.flowcontroller

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import app.cash.turbine.Turbine
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeFlowControllerConfirmationHandler(
    override val state: MutableStateFlow<ConfirmationHandler.State> =
        MutableStateFlow(ConfirmationHandler.State.Idle)
) : FlowControllerConfirmationHandler {
    override val confirmationHandler = FakeConfirmationHandler(
        hasReloadedFromProcessDeath = false,
        state = state,
    )

    override fun bootstrap(paymentMethodMetadata: PaymentMethodMetadata) {
        confirmationHandler.bootstrap(paymentMethodMetadata)
    }

    override fun register(
        activityResultCaller: ActivityResultCaller,
        lifecycleOwner: LifecycleOwner
    ) {
        confirmationHandler.register(activityResultCaller, lifecycleOwner)
    }

    override suspend fun start(arguments: ConfirmationHandler.Args) {
        confirmationHandler.start(arguments)
    }

    fun validate() {
        confirmationHandler.validate()
    }

    class Scenario(
        val handler: FlowControllerConfirmationHandler,
        val confirmationState: MutableStateFlow<ConfirmationHandler.State>,
        val registerTurbine: Turbine<FakeConfirmationHandler.RegisterCall>,
        val startTurbine: Turbine<ConfirmationHandler.Args>,
        val bootstrapTurbine: Turbine<FakeConfirmationHandler.BootstrapCall>
    )

    companion object {
        suspend fun test(
            initialState: ConfirmationHandler.State,
            block: suspend Scenario.() -> Unit,
        ) {
            val state = MutableStateFlow(initialState)

            val handler = FakeFlowControllerConfirmationHandler(
                state = state,
            )

            block(
                Scenario(
                    handler = handler,
                    confirmationState = state,
                    registerTurbine = handler.confirmationHandler.registerTurbine,
                    startTurbine = handler.confirmationHandler.startTurbine,
                    bootstrapTurbine = handler.confirmationHandler.bootstrapTurbine,
                )
            )

            handler.validate()
        }
    }
}
