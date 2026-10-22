package com.stripe.android.checkout

import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
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
    // TODO: we will need to update the error msging in here to reflect that calling confirm doesn't work if you haven't
    // configured PE.
    fun confirm() {
        lateinit var paymentSelection: PaymentSelection
        val arguments = operationCoordinator.tryBeginConfirmation {
            val state = stateHolder.state ?: return@tryBeginConfirmation Result.failure(
                IllegalStateException("Cannot confirm checkout before it is configured.")
            )
            paymentSelection = state.paymentSelection ?: return@tryBeginConfirmation Result.failure(
                IllegalStateException("Cannot confirm checkout without a selected payment option.")
            )
            val args = confirmationArgs(
                state = state,
                paymentSelection = paymentSelection,
            ) ?: return@tryBeginConfirmation Result.failure(
                IllegalStateException(
                    "Cannot confirm checkout because the selected payment option cannot be confirmed."
                )
            )
            Result.success(args)
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
        val paymentMethodMetadata = state.paymentMethodMetadata ?: return null
        val embeddedConfiguration = state.embeddedConfiguration ?: return null
        val configuration = commonConfigurationFactory.createForPaymentElement(
            configuration = state.configuration,
            checkoutSessionResponse = state.checkoutSessionResponse,
            collectedDetails = state.collectedDetails,
        ) ?: return null
        val confirmationOption = paymentSelection.toConfirmationOption(
            configuration = configuration,
            linkConfiguration = paymentMethodMetadata.linkState?.configuration,
            cardFundingFilter = paymentMethodMetadata.cardFundingFilter,
        )?.withSepaMandateAcknowledgement(
            hasAcknowledgedSepaMandate = paymentSelection.hasAcknowledgedSepaMandate ||
                !embeddedConfiguration.embeddedViewDisplaysMandateText,
        ) ?: return null

        return ConfirmationHandler.Args(
            confirmationOption = confirmationOption,
            paymentMethodMetadata = paymentMethodMetadata,
            statusBarColor = statusBarColor,
        )
    }

    private fun ConfirmationHandler.Option.withSepaMandateAcknowledgement(
        hasAcknowledgedSepaMandate: Boolean,
    ): ConfirmationHandler.Option {
        return if (this is PaymentMethodConfirmationOption.Saved) {
            copy(hasAcknowledgedSepaMandate = hasAcknowledgedSepaMandate)
        } else {
            this
        }
    }
}
