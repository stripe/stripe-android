package com.stripe.android.checkout

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.checkout.analytics.CheckoutEventReporter
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationTypeKey
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.utils.toConfirmationError
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class CheckoutAnalyticsPerformer @Inject constructor(
    private val confirmationHandler: ConfirmationHandler,
    private val checkoutEventReporter: CheckoutEventReporter,
    private val savedStateHandle: SavedStateHandle,
) {
    fun onPaymentElementConfirmationStarted(paymentSelection: PaymentSelection) {
        saveConfirmation(IntegrationType.PaymentElement, paymentSelection)
    }

    fun onExpressCheckoutElementConfirmationStarted(paymentSelection: PaymentSelection) {
        saveConfirmation(IntegrationType.ExpressCheckoutElement, paymentSelection)
    }

    suspend fun reportConfirmationResults() {
        confirmationHandler.state.collect { state ->
            if (state is ConfirmationHandler.State.Complete) {
                val integrationType = savedStateHandle.get<IntegrationType>(INTEGRATION_TYPE_KEY)
                val paymentSelection = savedStateHandle.get<PaymentSelection>(PAYMENT_SELECTION_KEY)

                if (integrationType != null && paymentSelection != null) {
                    reportConfirmationResult(state.result, paymentSelection)
                }

                clearConfirmation()
            }
        }
    }

    private fun reportConfirmationResult(
        result: ConfirmationHandler.Result,
        paymentSelection: PaymentSelection,
    ) {
        when (result) {
            is ConfirmationHandler.Result.Succeeded -> checkoutEventReporter.succeeded(
                paymentSelection = paymentSelection,
                deferredIntentConfirmationType = result.metadata[DeferredIntentConfirmationTypeKey],
                intentId = result.intent.id,
            )
            is ConfirmationHandler.Result.Failed -> result.toConfirmationError()?.let { error ->
                checkoutEventReporter.failed(paymentSelection, error)
            }
            is ConfirmationHandler.Result.Canceled -> Unit
        }
    }

    private fun saveConfirmation(
        integrationType: IntegrationType,
        paymentSelection: PaymentSelection,
    ) {
        savedStateHandle[INTEGRATION_TYPE_KEY] = integrationType
        savedStateHandle[PAYMENT_SELECTION_KEY] = paymentSelection
    }

    private fun clearConfirmation() {
        savedStateHandle.remove<IntegrationType>(INTEGRATION_TYPE_KEY)
        savedStateHandle.remove<PaymentSelection>(PAYMENT_SELECTION_KEY)
    }

    private enum class IntegrationType {
        PaymentElement,
        ExpressCheckoutElement,
    }

    private companion object {
        const val INTEGRATION_TYPE_KEY = "CheckoutAnalyticsPerformer.integrationType"
        const val PAYMENT_SELECTION_KEY = "CheckoutAnalyticsPerformer.paymentSelection"
    }
}
