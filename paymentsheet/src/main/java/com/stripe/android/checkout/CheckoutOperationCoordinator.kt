@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.ViewModelScope
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
    private val displayMessageResolver: CheckoutDisplayMessageResolver,
    private val confirmationHandler: ConfirmationHandler,
    private val sheetStateHolder: SheetStateHolder,
    private val sessionRefresher: CheckoutSessionRefresher,
    private val confirmationFinalizer: CheckoutConfirmationFinalizer,
    private val resultReporter: CheckoutConfirmationResultReporter,
    private val logger: Logger,
    private val resultCallback: CheckoutElementsController.ResultCallback,
    @ViewModelScope private val viewModelScope: CoroutineScope,
) {
    private val admissionLock = Any()
    private val pendingMutations = mutableListOf<Mutation>()
    private var confirmationInFlight = confirmationHandler.hasReloadedFromProcessDeath ||
        confirmationHandler.state.value is ConfirmationHandler.State.Confirming
    private var confirmationWasRestored = confirmationHandler.hasReloadedFromProcessDeath
    private var confirmationCompletionClaimed = false
    private val mutex = Mutex(locked = confirmationInFlight)

    private val _status = MutableStateFlow<Status>(
        if (confirmationInFlight) Status.Updating else Status.Idle
    )
    val status: StateFlow<Status> = _status.asStateFlow()

    sealed interface Status {
        data object Idle : Status
        data object Updating : Status
        data class Configuring(val configuration: CheckoutElementsController.Configuration.State) : Status
    }

    private data class Mutation(val status: Status)

    suspend fun <T> runMutation(
        block: suspend () -> Result<T>,
    ): Result<T> = runOperation(Status.Updating, block)

    suspend fun <T> runConfigure(
        configuration: CheckoutElementsController.Configuration.State,
        block: suspend () -> Result<T>,
    ): Result<T> = runOperation(Status.Configuring(configuration), block)

    private suspend fun <T> runOperation(
        status: Status,
        block: suspend () -> Result<T>,
    ): Result<T> {
        val mutation = Mutation(status)
        synchronized(admissionLock) {
            pendingMutations += mutation
            updateStatus()
        }

        return try {
            mutex.withLock {
                block()
            }
        } finally {
            synchronized(admissionLock) {
                val mutationIndex = pendingMutations.indexOfFirst { it === mutation }
                pendingMutations.removeAt(mutationIndex)
                updateStatus()
            }
        }
    }

    fun tryBeginConfirmation(
        arguments: () -> Result<ConfirmationHandler.Args>,
    ): ConfirmationHandler.Args? {
        val result = synchronized(admissionLock) {
            val rejectionMessage = when {
                sheetStateHolder.sheetIsOpen -> "Cannot confirm checkout while a payment flow is presented."
                pendingMutations.isNotEmpty() -> "Cannot confirm checkout while checkout mutations are in progress."
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
                updateStatus()
            }
        }
        // Integrator callbacks may re-enter checkout, so deliver rejections outside the admission lock.
        result.onFailure { error ->
            resultCallback.onResult(
                CheckoutElementsController.Result.Failed(
                    error = error,
                    displayMessage = displayMessageResolver.resolve(error),
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
                    val checkoutResult = result.asCheckoutResult(response, wasRestored, displayMessageResolver)
                    val reportedResult = if (
                        result is ConfirmationHandler.Result.Succeeded &&
                        checkoutResult is CheckoutElementsController.Result.Failed
                    ) {
                        ConfirmationHandler.Result.Failed(
                            cause = checkoutResult.error,
                            message = checkoutResult.error.stripeErrorMessage(),
                            type = ConfirmationHandler.Result.Failed.ErrorType.Internal,
                        )
                    } else {
                        result
                    }
                    resultReporter.reportConfirmationResult(reportedResult, response)
                    checkoutResult
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
        return confirmationFinalizer.finalize(response.withIntent(succeeded.intent)).fold(
            onSuccess = { finalized ->
                refreshSession { sessionRefresher.refresh(finalized) }
                result to finalized
            },
            onFailure = { error ->
                refreshSession { sessionRefresher.refresh() }
                val failed = ConfirmationHandler.Result.Failed(
                    cause = error,
                    message = error.stripeErrorMessage(),
                    type = ConfirmationHandler.Result.Failed.ErrorType.Transaction(),
                )
                failed to null
            },
        )
    }

    suspend fun failConfirmation(error: Throwable) {
        completeConfirmation {
            refreshSession { sessionRefresher.refresh() }
            CheckoutElementsController.Result.Failed(
                error = error,
                displayMessage = displayMessageResolver.resolve(error),
            )
        }
    }

    private suspend fun completeConfirmation(
        mapResult: suspend (confirmationWasRestored: Boolean) -> CheckoutElementsController.Result?,
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
                updateStatus()
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

    private fun updateStatus() = viewModelScope.launch(Dispatchers.Main) {
        synchronized(admissionLock) {
            val configuring = pendingMutations.firstNotNullOfOrNull { it.status as? Status.Configuring }
            _status.value = when {
                configuring != null -> configuring
                confirmationInFlight || pendingMutations.isNotEmpty() -> Status.Updating
                else -> Status.Idle
            }
        }
    }
}

private fun ConfirmationHandler.Result.asCheckoutResult(
    response: CheckoutSessionResponse?,
    confirmationWasRestored: Boolean,
    displayMessageResolver: CheckoutDisplayMessageResolver,
): CheckoutElementsController.Result? {
    return when (this) {
        is ConfirmationHandler.Result.Succeeded -> if (response != null) {
            CheckoutElementsController.Result.Completed(
                paymentStatus = response.paymentStatus.asPaymentStatus(),
            )
        } else {
            val error = IllegalStateException(
                "Checkout confirmation succeeded without returning the confirmed session."
            )
            CheckoutElementsController.Result.Failed(
                error = error,
                displayMessage = displayMessageResolver.resolve(error),
            )
        }
        is ConfirmationHandler.Result.Canceled -> when (action) {
            ConfirmationHandler.Result.Canceled.Action.InformCancellation ->
                CheckoutElementsController.Result.Canceled()
            ConfirmationHandler.Result.Canceled.Action.None ->
                CheckoutElementsController.Result.Canceled().takeIf { confirmationWasRestored }
            ConfirmationHandler.Result.Canceled.Action.ModifyPaymentDetails -> null
        }
        is ConfirmationHandler.Result.Failed -> CheckoutElementsController.Result.Failed(
            error = cause,
            displayMessage = displayMessageResolver.resolve(cause),
        )
    }
}
