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
    private var activeConfirmation = if (
        confirmationHandler.hasReloadedFromProcessDeath ||
        confirmationHandler.state.value is ConfirmationHandler.State.Confirming
    ) {
        ActiveConfirmation(
            wasRestored = confirmationHandler.hasReloadedFromProcessDeath,
            completionClaimed = false,
        )
    } else {
        null
    }
    private val mutex = Mutex(locked = activeConfirmation != null)

    private val _isUpdating = MutableStateFlow(activeConfirmation != null)
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
                activeConfirmation != null -> Result.failure(
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
    ): ConfirmationHandler.Args? {
        return synchronized(admissionLock) {
            if (sheetStateHolder.sheetIsOpen || pendingMutations > 0 || activeConfirmation != null) {
                return@synchronized null
            }
            val confirmationArguments = arguments() ?: return@synchronized null
            check(mutex.tryLock()) {
                "Checkout operation gate should be available after confirmation admission."
            }
            activeConfirmation = ActiveConfirmation(
                wasRestored = false,
                completionClaimed = false,
            )
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
        val confirmation = synchronized(admissionLock) {
            val confirmation = activeConfirmation
            if (confirmation == null || confirmation.completionClaimed) {
                return
            } else {
                confirmation.completionClaimed = true
                confirmation
            }
        }

        try {
            mapResult(confirmation.wasRestored)?.let(resultCallback::onResult)
        } finally {
            synchronized(admissionLock) {
                activeConfirmation = null
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
        _isUpdating.value = activeConfirmation != null || pendingMutations > 0
    }

    private class ActiveConfirmation(
        val wasRestored: Boolean,
        var completionClaimed: Boolean,
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
