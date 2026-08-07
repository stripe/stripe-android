@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
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
    private val resultCallback: CheckoutController.ResultCallback,
) {
    private val admissionLock = Any()
    private var pendingMutations = 0
    private var confirmationInFlight = confirmationHandler.hasReloadedFromProcessDeath ||
        confirmationHandler.state.value is ConfirmationHandler.State.Confirming
    private var confirmationCompletionClaimed = false
    private val mutex = Mutex()

    private val _isUpdating = MutableStateFlow(false)
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

    fun beginConfirmation(
        arguments: () -> ConfirmationHandler.Args?,
    ): ConfirmationHandler.Args? {
        return synchronized(admissionLock) {
            val confirmationArguments = arguments() ?: return@synchronized null
            confirmationInFlight = true
            confirmationArguments
        }
    }

    suspend fun observeConfirmationResults() {
        confirmationHandler.state.collect { state ->
            if (state is ConfirmationHandler.State.Complete) {
                completeConfirmation(state.result.asCheckoutResult())
            }
        }
    }

    fun failConfirmation(error: Throwable) {
        completeConfirmation(CheckoutController.Result.Failed(error))
    }

    private fun completeConfirmation(result: CheckoutController.Result) {
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
            resultCallback.onResult(result)
        } finally {
            synchronized(admissionLock) {
                confirmationInFlight = false
                confirmationCompletionClaimed = false
                updateIsUpdating()
            }
        }
    }

    private fun updateIsUpdating() {
        _isUpdating.value = pendingMutations > 0
    }
}

private fun ConfirmationHandler.Result.asCheckoutResult(): CheckoutController.Result {
    return when (this) {
        is ConfirmationHandler.Result.Succeeded -> CheckoutController.Result.Completed()
        is ConfirmationHandler.Result.Canceled -> CheckoutController.Result.Canceled()
        is ConfirmationHandler.Result.Failed -> CheckoutController.Result.Failed(cause)
    }
}
