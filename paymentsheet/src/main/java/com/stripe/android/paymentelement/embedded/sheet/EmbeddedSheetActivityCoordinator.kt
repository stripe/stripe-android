package com.stripe.android.paymentelement.embedded.sheet

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.stripe.android.common.ui.PaymentElementActivityResultCaller
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.isPaymentOptions

internal class EmbeddedSheetActivityCoordinator(
    private val activity: EmbeddedSheetActivity,
    initialArgs: EmbeddedActivityArgs,
    private val presentationFactory: EmbeddedSheetPresentationFactory,
) {
    private var state by mutableStateOf(
        State(
            args = initialArgs,
            presentation = presentationFactory.create(
                activity = activity,
                args = initialArgs,
                activityResultCaller = activity,
            ),
        ),
    )

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
        val updatedArgs = EmbeddedActivityArgs.fromIntent(intent) ?: return
        val isValidTransition =
            !activity.isFinishing &&
                state.args.presentationState == EmbeddedActivityArgs.PresentationState.Loading &&
                state.args.launchMode.isPaymentOptions &&
                updatedArgs.presentationState == EmbeddedActivityArgs.PresentationState.Ready &&
                updatedArgs.launchMode.isPaymentOptions
        if (!isValidTransition) return

        activity.intent = intent
        state.presentation.onDestroy()
        state = State(
            args = updatedArgs,
            presentation = presentationFactory.create(
                activity = activity,
                args = updatedArgs,
                activityResultCaller = PaymentElementActivityResultCaller(
                    key = "EmbeddedSheetActivity_${updatedArgs.paymentElementCallbackIdentifier}",
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
        val args: EmbeddedActivityArgs,
        val presentation: EmbeddedSheetPresentation,
    )
}
