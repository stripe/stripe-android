package com.stripe.android.checkout.ece

import com.stripe.android.checkout.CheckoutControllerState
import com.stripe.android.checkout.CheckoutControllerStateHolder
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayBillingEmailOverrideProvider
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayDisplayItemsFactory
import com.stripe.android.paymentelement.confirmation.toConfirmationOption
import com.stripe.android.payments.core.injection.STATUS_BAR_COLOR
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

internal interface ExpressCheckoutElementConfirmationPerformer {
    fun confirm(expressButton: ExpressButton)
}

internal class DefaultExpressCheckoutElementConfirmationPerformer @Inject constructor(
    private val stateHolder: CheckoutControllerStateHolder,
    private val confirmationHandler: ConfirmationHandler,
    @Named(STATUS_BAR_COLOR) private val statusBarColor: Int?,
    @ViewModelScope private val viewModelScope: CoroutineScope,
) : ExpressCheckoutElementConfirmationPerformer {
    override fun confirm(expressButton: ExpressButton) {
        val state = stateHolder.state ?: return
        val paymentSelection = expressButton.toSelection()

        val confirmationArgs = getConfirmationArgs(
            state = state,
            paymentSelection = paymentSelection,
        ) ?: return

        viewModelScope.launch {
            confirmationHandler.start(confirmationArgs)
        }
    }

    private fun getConfirmationArgs(
        state: CheckoutControllerState,
        paymentSelection: PaymentSelection,
    ): ConfirmationHandler.Args? {
        val configuration = state.embeddedConfiguration.asCommonConfiguration()
        val confirmationOption = paymentSelection.toConfirmationOption(
            configuration = configuration,
            linkConfiguration = state.paymentMethodMetadata.linkState?.configuration,
            cardFundingFilter = state.paymentMethodMetadata.cardFundingFilter,
            googlePayDisplayItems = GooglePayDisplayItemsFactory.create(state.paymentMethodMetadata),
            googlePayBillingEmailOverride = GooglePayBillingEmailOverrideProvider.get(
                configuration = configuration,
                paymentMethodMetadata = state.paymentMethodMetadata,
            ),
        ) ?: return null

        return ConfirmationHandler.Args(
            confirmationOption = confirmationOption,
            paymentMethodMetadata = state.paymentMethodMetadata,
            statusBarColor = statusBarColor,
        )
    }
}
