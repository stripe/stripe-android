package com.stripe.android.checkout

import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.intent.CheckoutSessionResponseKey
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutConfirmationResultHandler @Inject constructor(
    private val confirmationHandler: ConfirmationHandler,
    private val resultCallback: CheckoutController.ResultCallback,
    @ViewModelScope private val viewModelScope: CoroutineScope,
) {
    private var isAwaitingProcessDeathResult = confirmationHandler.hasReloadedFromProcessDeath

    /**
     * Observes the shared [ConfirmationHandler] for the controller's lifetime and forwards each
     * terminal confirmation result to [resultCallback]. This is registered exactly once (from
     * [CheckoutController]'s init); the handler emits [ConfirmationHandler.State.Complete] at most once
     * per confirmation and never restores a stale completion across process death, so this single
     * collector delivers each result exactly once.
     */
    fun register(
        onSucceeded: suspend (CheckoutSessionResponse?) -> Unit,
    ) {
        confirmationHandler.state
            .filterIsInstance<ConfirmationHandler.State.Complete>()
            .onEach { handle(it.result, onSucceeded) }
            .launchIn(viewModelScope)
    }

    private suspend fun handle(
        result: ConfirmationHandler.Result,
        onSucceeded: suspend (CheckoutSessionResponse?) -> Unit,
    ) {
        val isProcessDeathResult = isAwaitingProcessDeathResult
        isAwaitingProcessDeathResult = false

        when (result) {
            is ConfirmationHandler.Result.Succeeded -> handleSucceeded(result, onSucceeded)
            is ConfirmationHandler.Result.Failed ->
                resultCallback.onResult(CheckoutController.Result.Failed(result.cause))
            is ConfirmationHandler.Result.Canceled -> handleCanceled(result, isProcessDeathResult)
        }
    }

    private suspend fun handleSucceeded(
        result: ConfirmationHandler.Result.Succeeded,
        onSucceeded: suspend (CheckoutSessionResponse?) -> Unit,
    ) {
        // Synchronous confirmation carries the latest response. Next-action confirmation does not,
        // so the controller retrieves the completed Checkout Session before delivering the result.
        onSucceeded(result.metadata[CheckoutSessionResponseKey])
        resultCallback.onResult(CheckoutController.Result.Completed())
    }

    private fun handleCanceled(
        result: ConfirmationHandler.Result.Canceled,
        isProcessDeathResult: Boolean,
    ) {
        // None normally keeps the customer in the payment UI. After process death, however, it means
        // the pending next-action result did not return, so checkout must report the interrupted flow.
        val shouldInformMerchant =
            result.action == ConfirmationHandler.Result.Canceled.Action.InformCancellation ||
                result.action == ConfirmationHandler.Result.Canceled.Action.None && isProcessDeathResult

        if (shouldInformMerchant) {
            resultCallback.onResult(CheckoutController.Result.Canceled())
        }
    }
}
