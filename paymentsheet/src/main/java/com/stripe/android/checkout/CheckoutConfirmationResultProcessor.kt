package com.stripe.android.checkout

import com.stripe.android.core.Logger
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentsheet.repositories.CheckoutSessionRepository
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutConfirmationResultProcessor internal constructor(
    private val stateHolder: CheckoutControllerStateHolder,
    private val fetchResponse: suspend (sessionId: String, adaptivePricingAllowed: Boolean) ->
        Result<CheckoutSessionResponse>,
    private val reloadState: suspend (CheckoutControllerState) -> Unit,
    private val clearState: () -> Unit,
    private val logger: Logger,
) {
    @Inject
    constructor(
        stateHolder: CheckoutControllerStateHolder,
        checkoutSessionRepository: CheckoutSessionRepository,
        checkoutStateLoader: CheckoutStateLoader,
        logger: Logger,
    ) : this(
        stateHolder = stateHolder,
        fetchResponse = checkoutSessionRepository::init,
        reloadState = checkoutStateLoader::reload,
        clearState = checkoutStateLoader::clear,
        logger = logger,
    )

    suspend fun process(
        result: ConfirmationHandler.Result,
        confirmationWasRestored: Boolean,
    ): CheckoutController.Result? {
        return when (result) {
            is ConfirmationHandler.Result.Succeeded -> {
                clearState()
                CheckoutController.Result.Completed()
            }
            is ConfirmationHandler.Result.Failed -> {
                refresh()
                CheckoutController.Result.Failed(result.cause)
            }
            is ConfirmationHandler.Result.Canceled -> {
                refresh()
                result.asCheckoutResult(confirmationWasRestored)
            }
        }
    }

    suspend fun processFailure(error: Throwable): CheckoutController.Result.Failed {
        refresh()
        return CheckoutController.Result.Failed(error)
    }

    private suspend fun refresh() {
        val state = stateHolder.state ?: return

        try {
            val response = fetchResponse(
                state.checkoutSessionResponse.id,
                state.configuration.adaptivePricingAllowed,
            ).getOrThrow()
            reloadState(state.copy(checkoutSessionResponse = response))
        } catch (error: CancellationException) {
            throw error
        } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
            logger.error("Failed to refresh CheckoutController state after confirmation.", error)
        }
    }
}

@OptIn(CheckoutSessionPreview::class)
private fun ConfirmationHandler.Result.Canceled.asCheckoutResult(
    confirmationWasRestored: Boolean,
): CheckoutController.Result? {
    return when (action) {
        ConfirmationHandler.Result.Canceled.Action.InformCancellation -> CheckoutController.Result.Canceled()
        ConfirmationHandler.Result.Canceled.Action.None -> if (confirmationWasRestored) {
            CheckoutController.Result.Canceled()
        } else {
            null
        }
        ConfirmationHandler.Result.Canceled.Action.ModifyPaymentDetails -> null
    }
}
