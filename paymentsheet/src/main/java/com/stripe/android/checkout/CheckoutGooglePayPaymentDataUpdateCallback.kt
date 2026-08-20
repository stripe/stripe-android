package com.stripe.android.checkout

import android.content.Context
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataError
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdate
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdateCallback
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdateResponse
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import javax.inject.Inject

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutGooglePayPaymentDataUpdateCallback @Inject constructor(
    private val context: Context,
    private val taxRegionUpdater: CheckoutSessionTaxRegionUpdater,
    private val stateHolder: CheckoutControllerStateHolder,
    private val addressBuilder: CheckoutGooglePayPaymentDataUpdateAddressBuilder,
    private val mapper: CheckoutGooglePayPaymentDataUpdateMapper,
    private val errorReporter: ErrorReporter,
) : GooglePayPaymentDataUpdateCallback {
    override suspend fun onPaymentDataChanged(
        paymentDataUpdate: GooglePayPaymentDataUpdate,
    ): GooglePayPaymentDataUpdateResponse {
        val currentState = stateHolder.state ?: run {
            return notConfiguredResponse()
        }

        val googlePayConfiguration = currentState.commonConfiguration.googlePay ?: run {
            reportUnexpectedCallbackTrigger(VALUE_WITHOUT_CONFIG)

            return notConfiguredResponse()
        }

        return when (val trigger = paymentDataUpdate.callbackTrigger) {
            GooglePayPaymentDataUpdate.CallbackTrigger.Initialize,
            GooglePayPaymentDataUpdate.CallbackTrigger.ShippingAddress -> {
                updateShippingAddress(googlePayConfiguration, currentState, paymentDataUpdate)
            }
            GooglePayPaymentDataUpdate.CallbackTrigger.ShippingOption,
            GooglePayPaymentDataUpdate.CallbackTrigger.Offer -> {
                reportUnexpectedCallbackTrigger(
                    if (trigger == GooglePayPaymentDataUpdate.CallbackTrigger.ShippingOption) {
                        VALUE_SHIPPING_OPTION_TRIGGER
                    } else {
                        VALUE_OFFER_TRIGGER
                    }
                )

                mapToResponse(
                    configuration = googlePayConfiguration,
                    response = currentState.checkoutSessionResponse,
                    paymentDataUpdate = paymentDataUpdate,
                )
            }
        }
    }

    private suspend fun updateShippingAddress(
        googlePayConfiguration: PaymentSheet.GooglePayConfiguration,
        currentState: CheckoutControllerState,
        paymentDataUpdate: GooglePayPaymentDataUpdate,
    ): GooglePayPaymentDataUpdateResponse {
        val address = when (val result = addressBuilder.build(paymentDataUpdate)) {
            is CheckoutGooglePayPaymentDataUpdateAddressBuilder.Result.Unavailable -> {
                return mapToResponse(
                    configuration = googlePayConfiguration,
                    response = currentState.checkoutSessionResponse,
                    paymentDataUpdate = paymentDataUpdate,
                )
            }
            is CheckoutGooglePayPaymentDataUpdateAddressBuilder.Result.Available -> {
                result.address
            }
        }

        return taxRegionUpdater.updateServerStateIfNeeded(
            checkoutSessionResponse = currentState.checkoutSessionResponse,
            addressSource = CheckoutSessionResponse.TaxAddressSource.SHIPPING,
            address = address,
        ).fold(
            onSuccess = { response ->
                mapToResponse(googlePayConfiguration, response, paymentDataUpdate)
            },
            onFailure = { error ->
                GooglePayPaymentDataUpdateResponse(
                    newTransactionInfo = null,
                    error = GooglePayPaymentDataError(
                        reason = GooglePayPaymentDataError.Reason.ShippingAddressInvalid,
                        message = error.stripeErrorMessage(context),
                        intent = GooglePayPaymentDataError.Intent.ShippingAddress,
                    ),
                )
            },
        )
    }

    private fun mapToResponse(
        configuration: PaymentSheet.GooglePayConfiguration,
        response: CheckoutSessionResponse,
        paymentDataUpdate: GooglePayPaymentDataUpdate
    ): GooglePayPaymentDataUpdateResponse {
        return mapper.toResponse(configuration, response, paymentDataUpdate)
    }

    private fun reportUnexpectedCallbackTrigger(trigger: String) {
        errorReporter.report(
            errorEvent = ErrorReporter.UnexpectedErrorEvent.CHECKOUT_SESSION_GOOGLE_PAY_UNEXPECTED_CALLBACK_TRIGGER,
            additionalNonPiiParams = mapOf(
                FIELD_UNEXPECTED_TRIGGER_TYPE to trigger
            )
        )
    }

    private fun notConfiguredResponse(): GooglePayPaymentDataUpdateResponse {
        return GooglePayPaymentDataUpdateResponse(
            newTransactionInfo = null,
            error = GooglePayPaymentDataError(
                reason = GooglePayPaymentDataError.Reason.OtherError,
                message = context.getString(R.string.stripe_something_went_wrong),
                intent = GooglePayPaymentDataError.Intent.ShippingAddress,
            )
        )
    }

    private companion object {
        const val FIELD_UNEXPECTED_TRIGGER_TYPE = "unexpected_trigger_type"
        const val VALUE_SHIPPING_OPTION_TRIGGER = "shipping_option"
        const val VALUE_OFFER_TRIGGER = "offer"
        const val VALUE_WITHOUT_CONFIG = "without_config"
    }
}
