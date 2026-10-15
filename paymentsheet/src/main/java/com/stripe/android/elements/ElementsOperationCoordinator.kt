package com.stripe.android.elements

import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.utils.reportPaymentResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Serializes configuration and confirmation for all elements using the same intent. */
@Singleton
internal class ElementsOperationCoordinator @Inject constructor(
    private val stateHolder: ElementsControllerStateHolder,
    private val confirmationHandler: ConfirmationHandler,
    private val eventReporter: EventReporter,
    @ViewModelScope private val coroutineScope: CoroutineScope,
) {
    internal var statusBarColor: Int? = null

    private val updating = MutableStateFlow(confirmationHandler.state.value is ConfirmationHandler.State.Confirming)
    val isUpdating: StateFlow<Boolean> = updating.asStateFlow()
    private val results = Channel<PaymentSheetResult>(Channel.BUFFERED)
    val result = results.receiveAsFlow()

    init {
        coroutineScope.launch {
            confirmationHandler.state.collect { confirmationState ->
                when (confirmationState) {
                    is ConfirmationHandler.State.Complete -> {
                        updating.value = false
                        eventReporter.reportPaymentResult(confirmationState.result, stateHolder.state?.paymentSelection)
                        results.send(
                            when (val result = confirmationState.result) {
                                is ConfirmationHandler.Result.Succeeded -> PaymentSheetResult.Completed()
                                is ConfirmationHandler.Result.Canceled -> PaymentSheetResult.Canceled()
                                is ConfirmationHandler.Result.Failed -> PaymentSheetResult.Failed(result.cause)
                            }
                        )
                    }
                    is ConfirmationHandler.State.Confirming -> updating.value = true
                    ConfirmationHandler.State.Idle -> Unit
                }
            }
        }
    }

    suspend fun configure(block: suspend () -> Unit): Result<Unit> = withContext(Dispatchers.Main.immediate) {
        if (updating.value) {
            return@withContext Result.failure(IllegalStateException("ElementsController is busy."))
        }
        updating.value = true
        try {
            block()
            Result.success(Unit)
        } catch (error: CancellationException) {
            throw error
        } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
            Result.failure(error)
        } finally {
            updating.value = false
        }
    }

    fun confirm(
        selection: PaymentSelection,
        confirmationOption: ConfirmationHandler.Option,
    ) {
        val state = stateHolder.state ?: return
        if (updating.value) return
        updating.value = true
        stateHolder.state = state.copy(
            paymentElementState = state.paymentElementState.copy(paymentSelection = selection),
        )
        coroutineScope.launch {
            try {
                confirmationHandler.start(
                    ConfirmationHandler.Args(
                        confirmationOption = confirmationOption,
                        paymentMethodMetadata = state.paymentMethodMetadata,
                        statusBarColor = statusBarColor,
                    )
                )
            } catch (error: CancellationException) {
                updating.value = false
                throw error
            } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
                updating.value = false
                results.send(PaymentSheetResult.Failed(error))
            }
        }
    }
}
