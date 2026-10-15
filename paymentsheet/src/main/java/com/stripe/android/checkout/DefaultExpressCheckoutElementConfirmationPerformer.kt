package com.stripe.android.checkout

import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.elements.ece.ExpressButton
import com.stripe.android.elements.ece.ExpressCheckoutElementConfirmationPerformer
import com.stripe.android.elements.ece.toConfirmationOption
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.injection.STATUS_BAR_COLOR
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

internal class DefaultExpressCheckoutElementConfirmationPerformer @Inject constructor(
    private val stateHolder: CheckoutControllerStateHolder,
    private val confirmationHandler: ConfirmationHandler,
    private val operationCoordinator: CheckoutOperationCoordinator,
    private val analyticsPerformer: CheckoutAnalyticsPerformer,
    private val commonConfigurationFactory: CheckoutCommonConfigurationFactory,
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

        analyticsPerformer.onExpressCheckoutElementConfirmationStarted(expressButton.toSelection())

        viewModelScope.launch {
            try {
                confirmationHandler.start(confirmationArgs)
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
        val configuration = commonConfigurationFactory.createForExpressCheckoutElement(
            configuration = state.configuration,
            checkoutSessionResponse = state.checkoutSessionResponse,
            collectedDetails = state.collectedDetails,
        ) ?: return null
        val paymentMethodMetadata = state.expressCheckoutElementPaymentMethodMetadata ?: return null
        val shippingAddressRequired = (expressButton as? ExpressButton.GooglePay)?.shippingAddressRequired == true
        val shippingAddressParameters = if (shippingAddressRequired) {
            GooglePayJsonFactory.ShippingAddressParameters(
                isRequired = true,
                allowedCountryCodes = state.checkoutSessionResponse.allowedShippingCountries.orEmpty().toSet(),
            )
        } else {
            null
        }
        val confirmationOption = expressButton.toConfirmationOption(
            configuration = configuration,
            paymentMethodMetadata = paymentMethodMetadata,
            googlePayShippingAddressParameters = shippingAddressParameters,
        ) ?: return null

        return ConfirmationHandler.Args(
            confirmationOption = confirmationOption,
            paymentMethodMetadata = paymentMethodMetadata,
            statusBarColor = statusBarColor,
        )
    }
}
