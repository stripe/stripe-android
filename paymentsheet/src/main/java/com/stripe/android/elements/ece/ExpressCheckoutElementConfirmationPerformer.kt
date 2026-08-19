package com.stripe.android.elements.ece

import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.checkout.AnalyticsMetadata
import com.stripe.android.checkout.CheckoutControllerState
import com.stripe.android.checkout.CheckoutControllerStateHolder
import com.stripe.android.checkout.CheckoutOperationCoordinator
import com.stripe.android.checkout.IntegrationType
import com.stripe.android.checkout.asPaymentSheet
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayBillingEmailOverrideProvider
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
        val confirmationArgs = operationCoordinator.tryBeginConfirmation {
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
            try {
                val analyticsMetadata = AnalyticsMetadata(
                    integrationType = IntegrationType.ExpressCheckoutElement,
                    paymentSelection = TODO(),
                )
                stateHolder.setAnalyticsMetadata(analyticsMetadata)
                confirmationHandler.start(confirmationArgs)

                when (val result = confirmationHandler.awaitResult()) {
                    is ConfirmationHandler.Result.Succeeded -> eventReporter.onEcePaymentSuccess(expressButton)
                    is ConfirmationHandler.Result.Failed -> eventReporter.onEcePaymentFailure(expressButton, result)
                    is ConfirmationHandler.Result.Canceled,
                    null -> Unit
                }
            } catch (error: CancellationException) {
                throw error
            } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
                operationCoordinator.failConfirmation(error)
            }
        }
    }

    @OptIn(CheckoutSessionPreview::class)
    private fun getConfirmationArgs(
        state: CheckoutControllerState,
        expressButton: ExpressButton,
    ): ConfirmationHandler.Args? {
        val configuration = state.commonConfiguration.copy(
            link = state.configuration.expressCheckoutElementConfiguration.linkConfiguration.asPaymentSheet(),
            billingDetailsCollectionConfiguration = state.configuration.expressCheckoutElementConfiguration
                .billingDetailsCollectionConfiguration
                .asPaymentSheet(
                    requiresBillingAddress = state.checkoutSessionResponse.requiresBillingAddress
                ),
        )
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
