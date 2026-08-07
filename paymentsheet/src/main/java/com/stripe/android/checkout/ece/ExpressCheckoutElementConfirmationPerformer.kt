package com.stripe.android.checkout.ece

import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.checkout.CheckoutControllerState
import com.stripe.android.checkout.CheckoutControllerStateHolder
import com.stripe.android.checkout.CheckoutOperationCoordinator
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayBillingEmailOverrideProvider
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayDisplayItemsFactory
import com.stripe.android.paymentelement.confirmation.toConfirmationOption
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.injection.STATUS_BAR_COLOR
import kotlinx.coroutines.CancellationException
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
    private val operationCoordinator: CheckoutOperationCoordinator,
    private val eventReporter: ExpressCheckoutElementEventReporter,
    private val errorReporter: ErrorReporter,
    @Named(STATUS_BAR_COLOR) private val statusBarColor: Int?,
    @ViewModelScope private val viewModelScope: CoroutineScope,
) : ExpressCheckoutElementConfirmationPerformer {
    override fun confirm(expressButton: ExpressButton) {
        val operation = operationCoordinator.tryBeginConfirmation {
            val state = stateHolder.state ?: run {
                errorReporter.report(
                    ErrorReporter.UnexpectedErrorEvent.EXPRESS_CHECKOUT_ELEMENT_NULL_STATE_ON_CONFIRM
                )
                return@tryBeginConfirmation null
            }
            getConfirmationArgs(
                state = state,
                expressButton = expressButton,
            ) ?: run {
                errorReporter.report(
                    ErrorReporter.UnexpectedErrorEvent.EXPRESS_CHECKOUT_ELEMENT_NULL_CONFIRMATION_ARGS_ON_CONFIRM
                )
                null
            }
        } ?: return

        viewModelScope.launch {
            val result = try {
                confirmationHandler.start(operation.arguments)
                confirmationHandler.awaitResult()
            } catch (error: CancellationException) {
                throw error
            } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
                operationCoordinator.failConfirmation(operation, error)
                return@launch
            }

            // Analytics failures must not be reported to the integrator as confirmation failures.
            when (result) {
                is ConfirmationHandler.Result.Succeeded -> eventReporter.onEcePaymentSuccess(expressButton)
                is ConfirmationHandler.Result.Failed -> eventReporter.onEcePaymentFailure(expressButton, result)
                is ConfirmationHandler.Result.Canceled,
                null -> Unit
            }
        }
    }

    private fun getConfirmationArgs(
        state: CheckoutControllerState,
        expressButton: ExpressButton,
    ): ConfirmationHandler.Args? {
        val configuration = state.commonConfiguration
        val shippingAddressRequired = (expressButton as? ExpressButton.GooglePay)?.shippingAddressRequired == true
        val shippingAddressParameters = if (shippingAddressRequired) {
            GooglePayJsonFactory.ShippingAddressParameters(
                isRequired = true,
                allowedCountryCodes = state.checkoutSessionResponse.allowedShippingCountries.orEmpty().toSet(),
            )
        } else {
            null
        }
        val confirmationOption = expressButton.toSelection().toConfirmationOption(
            configuration = configuration,
            linkConfiguration = state.paymentMethodMetadata.linkState?.configuration,
            cardFundingFilter = state.paymentMethodMetadata.cardFundingFilter,
            googlePayDisplayItems = GooglePayDisplayItemsFactory.create(state.paymentMethodMetadata),
            googlePayBillingEmailOverride = GooglePayBillingEmailOverrideProvider.get(
                configuration = configuration,
                paymentMethodMetadata = state.paymentMethodMetadata,
            ),
            googlePayShippingAddressParameters = shippingAddressParameters,
        ) ?: return null

        return ConfirmationHandler.Args(
            confirmationOption = confirmationOption,
            paymentMethodMetadata = state.paymentMethodMetadata,
            statusBarColor = statusBarColor,
        )
    }
}
