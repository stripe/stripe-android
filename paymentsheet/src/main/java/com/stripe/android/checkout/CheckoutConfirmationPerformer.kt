package com.stripe.android.checkout

import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayBillingEmailOverrideProvider
import com.stripe.android.paymentelement.confirmation.toConfirmationOption
import com.stripe.android.payments.core.injection.STATUS_BAR_COLOR
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

internal class CheckoutConfirmationPerformer @Inject constructor(
    private val confirmationHandler: ConfirmationHandler,
    private val stateHolder: CheckoutControllerStateHolder,
    private val operationCoordinator: CheckoutOperationCoordinator,
    private val analyticsPerformer: CheckoutAnalyticsPerformer,
    @Named(STATUS_BAR_COLOR) private val statusBarColor: Int?,
    @ViewModelScope private val viewModelScope: CoroutineScope,
) {
    fun confirm() {
        var paymentSelection: PaymentSelection? = null
        val arguments = operationCoordinator.tryBeginConfirmation {
            confirmationArgs { paymentSelection = it }
        } ?: return
        analyticsPerformer.onPaymentElementConfirmationStarted(checkNotNull(paymentSelection))
        viewModelScope.launch {
            try {
                confirmationHandler.start(arguments)
            } catch (error: CancellationException) {
                throw error
            } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
                try {
                    operationCoordinator.failConfirmation(error)
                } finally {
                    analyticsPerformer.onConfirmationFinishedWithoutResult()
                }
            }
        }
    }

    private fun confirmationArgs(
        onPaymentSelection: (PaymentSelection) -> Unit,
    ): ConfirmationHandler.Args? {
        val state = stateHolder.state ?: return null
        val configuration = state.commonConfiguration
        val paymentSelection = state.paymentSelection ?: return null
        val confirmationOption = paymentSelection.toConfirmationOption(
            configuration = configuration,
            linkConfiguration = state.paymentMethodMetadata.linkState?.configuration,
            cardFundingFilter = state.paymentMethodMetadata.cardFundingFilter,
            googlePayBillingEmailOverride = GooglePayBillingEmailOverrideProvider.get(
                configuration = configuration,
                paymentMethodMetadata = state.paymentMethodMetadata,
            ),
        ) ?: return null
        onPaymentSelection(paymentSelection)

        return ConfirmationHandler.Args(
            confirmationOption = confirmationOption,
            paymentMethodMetadata = state.paymentMethodMetadata,
            statusBarColor = statusBarColor,
        )
    }
}
