@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.embedded.content.SheetStateHolder
import kotlinx.coroutines.CoroutineScope
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
    private val resultCallback: CheckoutController.ResultCallback,
    @ViewModelScope private val viewModelScope: CoroutineScope,
) {
    private val admissionLock = Any()
    private var pendingMutations = 0
    private val confirmationRestored = confirmationHandler.hasReloadedFromProcessDeath ||
        confirmationHandler.state.value is ConfirmationHandler.State.Confirming
    private var nextConfirmationId = if (confirmationRestored) 1L else 0L
    private var activeConfirmationId: Long? = if (confirmationRestored) 0L else null
    private val mutex = Mutex(locked = activeConfirmationId != null)

    private val _isUpdating = MutableStateFlow(activeConfirmationId != null)
    val isUpdating: StateFlow<Boolean> = _isUpdating.asStateFlow()

    init {
        viewModelScope.launch {
            observeConfirmationResults()
        }
    }

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
            if (
                sheetStateHolder.sheetIsOpen ||
                pendingMutations > 0 ||
                activeConfirmationId != null ||
                confirmationHandler.state.value is ConfirmationHandler.State.Confirming
            ) {
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
            updateIsUpdating()
            operation
        }
    }

    fun failConfirmation(
        operation: ConfirmationOperation,
        error: Throwable,
    ) {
        completeConfirmation(
            result = CheckoutController.Result.Failed(error),
            expectedConfirmationId = operation.id,
        )
    }

    private suspend fun observeConfirmationResults() {
        confirmationHandler.state.collect { state ->
            if (state is ConfirmationHandler.State.Complete) {
                completeConfirmation(
                    result = state.result.asCheckoutResult(),
                    expectedConfirmationId = null,
                )
            }
        }
    }

    private fun completeConfirmation(
        result: CheckoutController.Result,
        expectedConfirmationId: Long?,
    ) {
        val shouldComplete = synchronized(admissionLock) {
            val activeId = activeConfirmationId
            val completionIsStale = expectedConfirmationId != null && expectedConfirmationId != activeId
            if (activeId == null || completionIsStale) {
                false
            } else {
                activeConfirmationId = null
                mutex.unlock()
                updateIsUpdating()
                true
            }
        }
        if (!shouldComplete) return

        // The completed operation no longer owns the gate, so the callback may synchronously start another one.
        resultCallback.onResult(result)
    }

    private fun updateIsUpdating() {
        _isUpdating.value = activeConfirmationId != null || pendingMutations > 0
    }

    internal class ConfirmationOperation(
        internal val id: Long,
        internal val arguments: ConfirmationHandler.Args,
    )
}

private fun ConfirmationHandler.Result.asCheckoutResult(): CheckoutController.Result {
    return when (this) {
        is ConfirmationHandler.Result.Succeeded -> CheckoutController.Result.Completed()
        is ConfirmationHandler.Result.Canceled -> CheckoutController.Result.Canceled()
        is ConfirmationHandler.Result.Failed -> CheckoutController.Result.Failed(cause)
    }
}
