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
        arguments: () -> ConfirmationHandler.Args?,
    ): ConfirmationHandler.Args? {
        val result = synchronized(admissionLock) {
            val errorMessage = when {
                sheetStateHolder.sheetIsOpen ->
                    "Cannot confirm while a payment flow is presented."
                pendingMutations > 0 ->
                    "Cannot confirm while the checkout session is updating."
                confirmationInFlight ->
                    "Cannot confirm while another confirmation is in progress."
                else -> null
            }
            if (errorMessage != null) {
                return@synchronized Result.failure(IllegalStateException(errorMessage))
            }
            val confirmationArguments = arguments() ?: return@synchronized Result.failure(
                IllegalStateException(
                    "Cannot create confirmation arguments for the current payment selection."
                )
            )
            check(mutex.tryLock()) {
                "Checkout operation gate should be available after confirmation admission."
            }
            confirmationInFlight = true
            confirmationWasRestored = false
            updateIsUpdating()
            Result.success(confirmationArguments)
        }
        result.exceptionOrNull()?.let(::reportConfirmationFailure)
        return result.getOrNull()
    }

    fun reportConfirmationFailure(error: Throwable) {
        resultCallback.onResult(CheckoutController.Result.Failed(error))
    }

    suspend fun observeConfirmationResults() {
        confirmationHandler.state.collect { state ->
            if (state is ConfirmationHandler.State.Complete) {
                completeConfirmation { wasRestored ->
                    // A confirmation returning through an activity result has no fresh response to
                    // commit, but the session changed server-side, so re-fetch it.
                    val response = (state.result as? ConfirmationHandler.Result.Succeeded)
                        ?.metadata?.get(CheckoutSessionResponseKey)
                    refreshSession {
                        if (response != null) {
                            sessionRefresher.refresh(response)
                        } else {
                            sessionRefresher.refresh()
                        }
                    }
                    if (state.result.requestsPaymentDetailsModification()) {
                        sheetStateHolder.embeddedContentHelper?.presentPaymentOptions()
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

    private fun updateIsUpdating() {
        _isUpdating.value = confirmationInFlight || pendingMutations > 0
    }
}

private fun ConfirmationHandler.Result.requestsPaymentDetailsModification(): Boolean {
    return this is ConfirmationHandler.Result.Canceled &&
        action == ConfirmationHandler.Result.Canceled.Action.ModifyPaymentDetails
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
