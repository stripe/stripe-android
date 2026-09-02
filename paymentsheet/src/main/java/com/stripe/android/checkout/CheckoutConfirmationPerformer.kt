package com.stripe.android.checkout

import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.paymentelement.CheckoutSessionPreview
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
    private val commonConfigurationFactory: CheckoutCommonConfigurationFactory,
    @Named(STATUS_BAR_COLOR) private val statusBarColor: Int?,
    @ViewModelScope private val viewModelScope: CoroutineScope,
) {
    fun confirm() {
        val state = stateHolder.state ?: return
        val paymentSelection = state.paymentSelection ?: return
        val arguments = operationCoordinator.tryBeginConfirmation {
          confirmationArgs(
              state = state,
              paymentSelection = paymentSelection,
          )
        } ?: return
        analyticsPerformer.onPaymentElementConfirmationStarted(paymentSelection)
        viewModelScope.launch {
            try {
                confirmationHandler.start(arguments)
            } catch (error: CancellationException) {
                throw error
            } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
                operationCoordinator.failConfirmation(error)
            }
        }
    }

    @OptIn(CheckoutSessionPreview::class)
    private fun confirmationArgs(
        state: CheckoutControllerState,
        paymentSelection: PaymentSelection,
    ): ConfirmationHandler.Args? {
        val configuration = commonConfigurationFactory.createForPaymentElement(
            configuration = state.configuration,
            checkoutSessionResponse = state.checkoutSessionResponse,
            collectedDetails = state.collectedDetails,
        )
        val confirmationOption = paymentSelection.toConfirmationOption(
            configuration = configuration,
            linkConfiguration = state.paymentElementPaymentMethodMetadata.linkState?.configuration,
            cardFundingFilter = state.paymentElementPaymentMethodMetadata.cardFundingFilter,
            googlePayBillingEmailOverride = GooglePayBillingEmailOverrideProvider.get(
                configuration = configuration,
                paymentMethodMetadata = state.paymentElementPaymentMethodMetadata,
            ),
        ) ?: return null

        return ConfirmationHandler.Args(
            confirmationOption = confirmationOption,
            paymentMethodMetadata = state.paymentElementPaymentMethodMetadata,
            statusBarColor = statusBarColor,
        )
    }
}
