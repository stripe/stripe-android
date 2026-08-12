@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

import com.stripe.android.core.exception.StripeException
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.intent.CheckoutSessionResponseKey
import com.stripe.android.paymentelement.embedded.content.SheetStateHolder
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

private val CONFIRMATION_REFRESH_TIMEOUT_MS = 10.seconds.inWholeMilliseconds

@Singleton
internal class CheckoutOperationCoordinator @Inject constructor(
    private val confirmationHandler: ConfirmationHandler,
    private val sheetStateHolder: SheetStateHolder,
    private val resultCallback: CheckoutController.ResultCallback,
    private val errorReporter: ErrorReporter,
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

    suspend fun observeConfirmationResults(
        refreshCheckoutSession: suspend (CheckoutSessionResponse) -> Unit,
    ) {
        confirmationHandler.state.collect { state ->
            if (state is ConfirmationHandler.State.Complete) {
                completeConfirmation(state.result, refreshCheckoutSession)
            }
        }
    }

    fun failConfirmation(error: Throwable) {
        if (claimCompletion()) {
            deliverAndRelease(CheckoutController.Result.Failed(error))
        }
    }

    private suspend fun completeConfirmation(
        result: ConfirmationHandler.Result,
        refreshCheckoutSession: suspend (CheckoutSessionResponse) -> Unit,
    ) {
        // Claim before refreshing so a racing failConfirmation cannot deliver a second result.
        if (!claimCompletion()) return

        val confirmedSession = (result as? ConfirmationHandler.Result.Succeeded)
            ?.metadata
            ?.get(CheckoutSessionResponseKey)

        if (confirmedSession != null) {
            refreshOrReport(refreshCheckoutSession, confirmedSession)
        }

        deliverAndRelease(result.asCheckoutResult())
    }

    private suspend fun refreshOrReport(
        refreshCheckoutSession: suspend (CheckoutSessionResponse) -> Unit,
        confirmedSession: CheckoutSessionResponse,
    ) {
        try {
            withTimeout(CONFIRMATION_REFRESH_TIMEOUT_MS) {
                refreshCheckoutSession(confirmedSession)
            }
        } catch (@Suppress("TooGenericExceptionCaught") error: Throwable) {
            // Losing our own scope (e.g. destroy()) must not deliver a result, so let that
            // cancellation through. Everything else is a refresh failure — including a timeout and
            // a CancellationException the reload surfaced as a value, neither of which cancelled
            // us. Rethrowing those would strand the gate locked with no result ever delivered.
            currentCoroutineContext().ensureActive()
            errorReporter.report(
                errorEvent = ErrorReporter.UnexpectedErrorEvent.CHECKOUT_CONFIRMATION_SESSION_REFRESH_FAILED,
                stripeException = StripeException.create(error),
            )
        }
    }

    private fun claimCompletion(): Boolean = synchronized(admissionLock) {
        if (!confirmationInFlight || confirmationCompletionClaimed) {
            false
        } else {
            confirmationCompletionClaimed = true
            true
        }
    }

    private fun deliverAndRelease(result: CheckoutController.Result) {
        try {
            resultCallback.onResult(result)
        } finally {
            synchronized(admissionLock) {
                confirmationInFlight = false
                confirmationCompletionClaimed = false
                mutex.unlock()
                updateIsUpdating()
            }
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
