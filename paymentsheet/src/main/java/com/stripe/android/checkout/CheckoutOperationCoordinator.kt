@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

import com.stripe.android.core.Logger
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
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
    private var confirmationInFlight = confirmationHandler.hasReloadedFromProcessDeath ||
        confirmationHandler.state.value is ConfirmationHandler.State.Confirming
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
        arguments: () -> ConfirmationHandler.Args?,
    ): ConfirmationHandler.Args? {
        val admission = synchronized(admissionLock) {
            when {
                sheetStateHolder.sheetIsOpen -> ConfirmationAdmission.Rejected(
                    IllegalStateException("Cannot confirm checkout session while a payment flow is presented.")
                )
                pendingMutations > 0 || confirmationInFlight -> ConfirmationAdmission.Rejected(
                    IllegalStateException("Cannot confirm checkout session while another mutation is in progress.")
                )
                else -> {
                    val confirmationArguments = arguments()
                        ?: return@synchronized ConfirmationAdmission.NoArguments
                    check(mutex.tryLock()) {
                        "Checkout operation gate should be available after confirmation admission."
                    }
                    confirmationInFlight = true
                    updateIsUpdating()
                    ConfirmationAdmission.Accepted(confirmationArguments)
                }
            }
        }

        return when (admission) {
            is ConfirmationAdmission.Accepted -> admission.arguments
            is ConfirmationAdmission.Rejected -> {
                resultCallback.onResult(CheckoutController.Result.Failed(admission.error))
                null
            }
            is ConfirmationAdmission.NoArguments -> null
        }
    }

    suspend fun observeConfirmationResults() {
        confirmationHandler.state.collect { state ->
            if (state is ConfirmationHandler.State.Complete) {
                completeConfirmation {
                    if (state.result !is ConfirmationHandler.Result.Succeeded) {
                        refreshSession()
                    }
                    state.result.asCheckoutResult()
                }
            }
        }
    }

    suspend fun failConfirmation(error: Throwable) {
        completeConfirmation {
            refreshSession()
            CheckoutController.Result.Failed(error)
        }
    }

    private suspend fun completeConfirmation(result: suspend () -> CheckoutController.Result) {
        val shouldComplete = synchronized(admissionLock) {
            if (!confirmationInFlight || confirmationCompletionClaimed) {
                false
            } else {
                confirmationCompletionClaimed = true
                true
            }
        }
        if (!shouldComplete) return

        try {
            resultCallback.onResult(result())
        } finally {
            synchronized(admissionLock) {
                confirmationInFlight = false
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
    private suspend fun refreshSession() {
        try {
            sessionRefresher.refresh()
        } catch (error: CancellationException) {
            throw error
        } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
            logger.error("Failed to refresh the checkout session after confirmation.", error)
        }
    }

    private fun updateIsUpdating() {
        _isUpdating.value = confirmationInFlight || pendingMutations > 0
    }

    private sealed interface ConfirmationAdmission {
        data class Accepted(val arguments: ConfirmationHandler.Args) : ConfirmationAdmission
        data class Rejected(val error: Throwable) : ConfirmationAdmission
        data object NoArguments : ConfirmationAdmission
    }
}

private fun ConfirmationHandler.Result.asCheckoutResult(): CheckoutController.Result {
    return when (this) {
        is ConfirmationHandler.Result.Succeeded -> CheckoutController.Result.Completed()
        is ConfirmationHandler.Result.Canceled -> CheckoutController.Result.Canceled()
        is ConfirmationHandler.Result.Failed -> CheckoutController.Result.Failed(cause)
    }
}
