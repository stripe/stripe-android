@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.LocalStripeException
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.model.parsers.StripeErrorJsonParser
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.intent.CheckoutSessionResponseKey
import com.stripe.android.paymentelement.embedded.content.SheetStateHolder
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class CheckoutOperationCoordinator @Inject constructor(
    private val confirmationHandler: ConfirmationHandler,
    private val sheetStateHolder: SheetStateHolder,
    private val sessionRefresher: CheckoutSessionRefresher,
    private val confirmationFinalizer: CheckoutConfirmationFinalizer,
    private val resultReporter: CheckoutConfirmationResultReporter,
    private val logger: Logger,
    private val resultCallback: CheckoutController.ResultCallback,
    @ViewModelScope private val viewModelScope: CoroutineScope,
) {
    private val admissionLock = Any()
    private var pendingMutations = 0
    private var confirmationInFlight = confirmationHandler.hasReloadedFromProcessDeath ||
        confirmationHandler.state.value is ConfirmationHandler.State.Confirming
    private var confirmationWasRestored = confirmationHandler.hasReloadedFromProcessDeath
    private var confirmationCompletionClaimed = false
    private val mutex = Mutex(locked = confirmationInFlight)

    private val _isUpdating = MutableStateFlow(confirmationInFlight)
    val isUpdating: StateFlow<Boolean> = _isUpdating.asStateFlow()

    suspend fun <T> runMutation(
        block: suspend () -> Result<T>,
    ): Result<T> {
        synchronized(admissionLock) {
            pendingMutations += 1
            updateIsUpdating()
        }

        return try {
            mutex.withLock {
                block()
            }
        } finally {
            synchronized(admissionLock) {
                pendingMutations -= 1
                updateIsUpdating()
            }
        }
    }

    fun tryBeginConfirmation(
        arguments: () -> Result<ConfirmationHandler.Args>,
    ): ConfirmationHandler.Args? {
        val result = synchronized(admissionLock) {
            val rejectionMessage = when {
                sheetStateHolder.sheetIsOpen -> "Cannot confirm checkout while a payment flow is presented."
                pendingMutations > 0 -> "Cannot confirm checkout while checkout mutations are in progress."
                confirmationInFlight -> "Cannot confirm checkout while another confirmation is in progress."
                else -> null
            }
            if (rejectionMessage != null) {
                return@synchronized Result.failure<ConfirmationHandler.Args>(IllegalStateException(rejectionMessage))
            }
            arguments().onSuccess {
                check(mutex.tryLock()) {
                    "Checkout operation gate should be available after confirmation admission."
                }
                confirmationInFlight = true
                confirmationWasRestored = false
                updateIsUpdating()
            }
        }
        // Integrator callbacks may re-enter checkout, so deliver rejections outside the admission lock.
        result.onFailure { error ->
            resultCallback.onResult(
                CheckoutController.Result.Failed(
                    error = error,
                    displayMessage = error.checkoutDisplayMessage(),
                )
            )
        }
        return result.getOrNull()
    }

    suspend fun observeConfirmationResults() {
        confirmationHandler.state.collect { state ->
            if (state is ConfirmationHandler.State.Complete) {
                completeConfirmation { wasRestored ->
                    val (result, response) = finalizeConfirmation(state.result)
                    resultReporter.reportConfirmationResult(result, response)
                    result.asCheckoutResult(wasRestored)
                }
            }
        }
    }

    /**
     * Resolves an accepted confirmation to its final server state and commits it. Failed and canceled
     * confirmations leave the session open, so the current session is re-fetched instead.
     */
    private suspend fun finalizeConfirmation(
        result: ConfirmationHandler.Result,
    ): Pair<ConfirmationHandler.Result, CheckoutSessionResponse?> {
        val succeeded = result as? ConfirmationHandler.Result.Succeeded
        val response = succeeded?.metadata?.get(CheckoutSessionResponseKey)
        if (response == null) {
            refreshSession { sessionRefresher.refresh() }
            return result to null
        }

        // A next action returns a newer Intent than the one retrieved after /confirm.
        val finalized = confirmationFinalizer.finalize(response.withIntent(succeeded.intent))
        refreshSession { sessionRefresher.refresh(finalized.response) }
        val finalResult = when (finalized) {
            is CheckoutConfirmationFinalizer.Result.Completed -> result
            is CheckoutConfirmationFinalizer.Result.Failed -> ConfirmationHandler.Result.Failed(
                cause = finalized.error,
                message = finalized.error.stripeErrorMessage(),
                type = ConfirmationHandler.Result.Failed.ErrorType.Transaction(),
            )
        }
        return finalResult to finalized.response
    }

    suspend fun failConfirmation(error: Throwable) {
        completeConfirmation {
            refreshSession { sessionRefresher.refresh() }
            CheckoutController.Result.Failed(
                error = error,
                displayMessage = error.checkoutDisplayMessage(),
            )
        }
    }

    private suspend fun completeConfirmation(
        mapResult: suspend (confirmationWasRestored: Boolean) -> CheckoutController.Result?,
    ) {
        val wasRestored = synchronized(admissionLock) {
            if (!confirmationInFlight || confirmationCompletionClaimed) {
                return
            } else {
                confirmationCompletionClaimed = true
                confirmationWasRestored
            }
        }

        try {
            mapResult(wasRestored)?.let(resultCallback::onResult)
        } finally {
            synchronized(admissionLock) {
                confirmationInFlight = false
                confirmationWasRestored = false
                confirmationCompletionClaimed = false
                mutex.unlock()
                updateIsUpdating()
            }
        }
    }

    /**
     * The refreshed state has to be committed before the result reaches the integrator, but a failed
     * refresh must not replace the confirmation result the integrator is waiting on.
     */
    private suspend fun refreshSession(refresh: suspend () -> Unit) {
        try {
            refresh()
        } catch (error: CancellationException) {
            throw error
        } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
            logger.error("Failed to refresh the checkout session after confirmation.", error)
        }
    }

    private fun updateIsUpdating() = viewModelScope.launch(Dispatchers.Main) {
        _isUpdating.value = confirmationInFlight || pendingMutations > 0
    }
}

private fun ConfirmationHandler.Result.asCheckoutResult(
    confirmationWasRestored: Boolean,
): CheckoutController.Result? {
    return when (this) {
        is ConfirmationHandler.Result.Succeeded -> CheckoutController.Result.Completed()
        is ConfirmationHandler.Result.Canceled -> when (action) {
            ConfirmationHandler.Result.Canceled.Action.InformCancellation ->
                CheckoutController.Result.Canceled()
            ConfirmationHandler.Result.Canceled.Action.None ->
                CheckoutController.Result.Canceled().takeIf { confirmationWasRestored }
            ConfirmationHandler.Result.Canceled.Action.ModifyPaymentDetails -> null
        }
        is ConfirmationHandler.Result.Failed -> CheckoutController.Result.Failed(
            error = cause,
            displayMessage = cause.checkoutDisplayMessage(),
        )
    }
}

internal fun Throwable.checkoutDisplayMessage(): String? {
    (this as? LocalStripeException)?.displayMessage?.let { return it }

    // Malformed responses contain a parser diagnostic, so omit that message.
    return (this as? StripeException)?.stripeError?.message
        ?.takeIf { it != StripeErrorJsonParser.MALFORMED_RESPONSE_MESSAGE }
}
