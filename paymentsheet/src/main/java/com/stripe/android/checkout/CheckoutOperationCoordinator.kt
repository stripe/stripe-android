@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

import com.stripe.android.core.Logger
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.intent.CheckoutSessionResponseKey
import com.stripe.android.paymentelement.embedded.content.SheetStateHolder
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.utils.reportPaymentResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class CheckoutOperationCoordinator @Inject constructor(
    private val confirmationHandler: ConfirmationHandler,
    private val sheetStateHolder: SheetStateHolder,
    private val sessionRefresher: CheckoutSessionRefresher,
    private val logger: Logger,
    private val resultCallback: CheckoutController.ResultCallback,
    private val eventReporter: EventReporter,
) {
    private val admissionLock = Any()
    private var pendingMutations = 0
    private var confirmationInFlight = confirmationHandler.hasReloadedFromProcessDeath ||
        confirmationHandler.state.value is ConfirmationHandler.State.Confirming
    private var confirmationWasRestored = confirmationHandler.hasReloadedFromProcessDeath
    private var confirmationCompletionClaimed = false
    private var selectionToConfirm: PaymentSelection? = null
    private var surfaceConfirmingFrom: Surface? = null
    private val mutex = Mutex(locked = confirmationInFlight)

    enum class Surface {
        ECE,
        PE,
    }

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

    fun <T> runSynchronousMutation(
        block: () -> Result<T>,
    ): Result<T> {
        return synchronized(admissionLock) {
            when {
                confirmationInFlight -> Result.failure(
                    IllegalStateException("Cannot mutate checkout session while confirmation is in progress.")
                )
                pendingMutations > 0 -> Result.failure(
                    IllegalStateException("Cannot mutate checkout session while another mutation is in progress.")
                )
                else -> {
                    check(mutex.tryLock()) {
                        "Checkout operation gate should be available after synchronous mutation admission."
                    }
                    try {
                        block()
                    } finally {
                        mutex.unlock()
                    }
                }
            }
        }
    }

    fun tryBeginConfirmation(
        selection: PaymentSelection,
        arguments: () -> ConfirmationHandler.Args?,
    ): ConfirmationHandler.Args? {
        return synchronized(admissionLock) {
            if (sheetStateHolder.sheetIsOpen || pendingMutations > 0 || confirmationInFlight) {
                return@synchronized null
            }
            val confirmationArguments = arguments() ?: return@synchronized null
            check(mutex.tryLock()) {
                "Checkout operation gate should be available after confirmation admission."
            }
            this.selectionToConfirm = selection
            confirmationInFlight = true
            confirmationWasRestored = false
            updateIsUpdating()
            confirmationArguments
        }
    }

    suspend fun observeConfirmationResults() {
        confirmationHandler.state.collect { state ->
            if (state is ConfirmationHandler.State.Complete) {
                completeConfirmation { wasRestored ->
                    when (val result = state.result) {
                        is ConfirmationHandler.Result.Succeeded -> {
                            result.metadata[CheckoutSessionResponseKey]?.let { response ->
                                refreshSession { sessionRefresher.refresh(response) }
                            }
                        }

                        else -> refreshSession { sessionRefresher.refresh() }
                    }
                    state.result.asCheckoutResult(wasRestored)
                }
            }
        }
    }

    suspend fun failConfirmation(error: Throwable) {
        completeConfirmation {
            refreshSession { sessionRefresher.refresh() }
            CheckoutController.Result.Failed(error)
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
            mapResult(wasRestored)?.let {
                // TODO: maybe it doesn't belong quite here, we need the confirmation handler result.
                // TODO: we'd also pass the surface in here, so that we can track whether the payment came from ECE or PE.
//                eventReporter.reportPaymentResult(it, selectionToConfirm)
                resultCallback.onResult(it)
            }
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

    private fun updateIsUpdating() {
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
        is ConfirmationHandler.Result.Failed -> CheckoutController.Result.Failed(cause)
    }
}
