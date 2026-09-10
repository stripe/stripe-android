package com.stripe.android.paymentelement.embedded.sheet

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.stripe.android.common.ui.PaymentElementActivityResultCaller
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.EmbeddedActivityState
import com.stripe.android.paymentelement.embedded.toState

internal class EmbeddedSheetActivityCoordinator(
    private val activity: EmbeddedSheetActivity,
    initialState: EmbeddedActivityState,
    private val presentationFactory: EmbeddedSheetPresentationFactory,
) {
    private var state by mutableStateOf(
        State(
            state = initialState,
            presentation = presentationFactory.create(
                activity = activity,
                state = initialState,
                activityResultCaller = activity,
            ),
        ),
    )

    val currentState: EmbeddedActivityState
        get() = state.state

    fun register() {
        state.presentation.register()
    }

    fun canDismiss(): Boolean {
        return state.presentation.canDismiss()
    }

    fun onDismissed() {
        state.presentation.onDismissed()
    }

    @Composable
    fun Content() {
        state.presentation.Content()
    }

    fun handleNewIntent(intent: Intent) {
        val updatedState = EmbeddedActivityArgs.fromIntent(intent)?.toState() ?: return
        val isValidTransition = !activity.isFinishing &&
            state.state is EmbeddedActivityState.LoadingPaymentOptions &&
            updatedState is EmbeddedActivityState.Ready.PaymentOptions
        if (!isValidTransition) return

        activity.intent = intent
        state.presentation.onDestroy()
        state = State(
            state = updatedState,
            presentation = presentationFactory.create(
                activity = activity,
                state = updatedState,
                activityResultCaller = PaymentElementActivityResultCaller(
                    key = "EmbeddedSheetActivity_${updatedState.context.paymentElementCallbackIdentifier}",
                    registryOwner = activity,
                ),
            ),
        )
        state.presentation.register()
    }

    fun onDestroy() {
        state.presentation.onDestroy()
    }

    private data class State(
        val state: EmbeddedActivityState,
        val presentation: EmbeddedSheetPresentation,
    )
}
