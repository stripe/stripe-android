@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

import com.stripe.android.core.Logger
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.intent.CheckoutSessionResponseKey
import com.stripe.android.paymentelement.embedded.content.SheetStateHolder
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
) {
    private val admissionLock = Any()
    private var pendingMutations = 0
    private val confirmationRestored = confirmationHandler.hasReloadedFromProcessDeath ||
        confirmationHandler.state.value is ConfirmationHandler.State.Confirming
    private var confirmationWasRestored = confirmationHandler.hasReloadedFromProcessDeath
    private var nextConfirmationId = if (confirmationRestored) 1L else 0L
    private var activeConfirmationId: Long? = if (confirmationRestored) 0L else null
    private var confirmationCompletionClaimed = false
    private val mutex = Mutex(locked = activeConfirmationId != null)

    private val _isUpdating = MutableStateFlow(activeConfirmationId != null)
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
                activeConfirmationId != null -> Result.failure(
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
        arguments: () -> ConfirmationHandler.Args?,
    ): ConfirmationOperation? {
        return synchronized(admissionLock) {
            if (sheetStateHolder.sheetIsOpen || pendingMutations > 0 || activeConfirmationId != null) {
                return@synchronized null
            }
            val confirmationArguments = arguments() ?: return@synchronized null
            check(mutex.tryLock()) {
                "Checkout operation gate should be available after confirmation admission."
            }
            val operation = ConfirmationOperation(
                id = nextConfirmationId++,
                arguments = confirmationArguments,
            )
            activeConfirmationId = operation.id
            confirmationWasRestored = false
            updateIsUpdating()
            operation
        }
    }

    suspend fun observeConfirmationResults() {
        confirmationHandler.state.collect { state ->
            if (state is ConfirmationHandler.State.Complete) {
                completeConfirmation(expectedConfirmationId = null) { wasRestored ->
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

    suspend fun failConfirmation(
        operation: ConfirmationOperation,
        error: Throwable,
    ) {
        completeConfirmation(expectedConfirmationId = operation.id) {
            refreshSession { sessionRefresher.refresh() }
            CheckoutController.Result.Failed(error)
        }
    }

    private suspend fun completeConfirmation(
        expectedConfirmationId: Long?,
        result: suspend (confirmationWasRestored: Boolean) -> CheckoutController.Result?,
    ) {
        val wasRestored = synchronized(admissionLock) {
            val activeId = activeConfirmationId
            val completionIsStale = expectedConfirmationId != null && expectedConfirmationId != activeId
            if (activeId == null || completionIsStale || confirmationCompletionClaimed) {
                return
            } else {
                confirmationCompletionClaimed = true
                confirmationWasRestored
            }
        }

        val checkoutResult = try {
            result(wasRestored)
        } finally {
            synchronized(admissionLock) {
                activeConfirmationId = null
                confirmationWasRestored = false
                confirmationCompletionClaimed = false
                mutex.unlock()
                updateIsUpdating()
            }
        }

        checkoutResult?.let(resultCallback::onResult)
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
        _isUpdating.value = activeConfirmationId != null || pendingMutations > 0
    }

    internal class ConfirmationOperation(
        internal val id: Long,
        internal val arguments: ConfirmationHandler.Args,
    )
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
