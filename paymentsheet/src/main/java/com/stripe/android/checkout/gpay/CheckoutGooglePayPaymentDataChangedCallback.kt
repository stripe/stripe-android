package com.stripe.android.checkout.gpay

import android.content.Context
import com.stripe.android.checkout.CheckoutController
import com.stripe.android.checkout.CheckoutControllerStateHolder
import com.stripe.android.core.exception.StripeException
import com.stripe.android.googlepaylauncher.callback.GooglePayIntermediatePaymentData
import com.stripe.android.googlepaylauncher.callback.GooglePayPaymentDataChangedCallback
import com.stripe.android.googlepaylauncher.callback.GooglePayPaymentDataError
import com.stripe.android.googlepaylauncher.callback.GooglePayPaymentDataRequestUpdate
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.payments.core.analytics.ErrorReporter
import javax.inject.Inject

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutGooglePayPaymentDataChangedCallback @Inject constructor(
    private val checkoutController: CheckoutController,
    private val stateHolder: CheckoutControllerStateHolder,
    private val context: Context,
    private val errorReporter: ErrorReporter,
) : GooglePayPaymentDataChangedCallback {
    override suspend fun onPaymentDataChanged(
        intermediatePaymentData: GooglePayIntermediatePaymentData,
    ): GooglePayPaymentDataRequestUpdate {
        return when (intermediatePaymentData.callbackTrigger) {
            GooglePayIntermediatePaymentData.CallbackTrigger.SHIPPING_ADDRESS -> {
                updateShippingAddress(intermediatePaymentData)
            }
            GooglePayIntermediatePaymentData.CallbackTrigger.INITIALIZE,
            GooglePayIntermediatePaymentData.CallbackTrigger.SHIPPING_OPTION,
            GooglePayIntermediatePaymentData.CallbackTrigger.OFFER,
            null -> {
                createRequestUpdateFromCurrentState(intermediatePaymentData)
            }
        }
    }

    private suspend fun updateShippingAddress(
        intermediatePaymentData: GooglePayIntermediatePaymentData,
    ): GooglePayPaymentDataRequestUpdate {
        val shippingAddress = intermediatePaymentData.shippingAddress
            ?: return GooglePayPaymentDataRequestUpdate(
                error = GooglePayPaymentDataError(
                    reason = GooglePayPaymentDataError.Reason.SHIPPING_ADDRESS_INVALID,
                    message = "Shipping address is required.",
                    intent = GooglePayPaymentDataError.Intent.SHIPPING_ADDRESS,
                ),
            )

        if (shippingAddress.countryCode.isBlank()) {
            return GooglePayPaymentDataRequestUpdate(
                error = GooglePayPaymentDataError(
                    reason = GooglePayPaymentDataError.Reason.SHIPPING_ADDRESS_INVALID,
                    message = "Country is required.",
                    intent = GooglePayPaymentDataError.Intent.SHIPPING_ADDRESS,
                ),
            )
        }

        val address = CheckoutController.Address()
            .country(shippingAddress.countryCode)
            .city(shippingAddress.locality.takeIf { it.isNotEmpty() })
            .postalCode(shippingAddress.postalCode.takeIf { it.isNotEmpty() })
            .state(
                shippingAddress.iso3166AdministrativeArea?.takeIf { it.isNotEmpty() }
                    ?: shippingAddress.administrativeArea.takeIf { it.isNotEmpty() },
            )

        return checkoutController.updateShippingAddress(
            name = null,
            phoneNumber = null,
            address = address,
        ).fold(
            onSuccess = {
                createRequestUpdateFromCurrentState(intermediatePaymentData)
            },
            onFailure = { error ->
                errorReporter.report(
                    ErrorReporter.UnexpectedErrorEvent.GOOGLE_PAY_DYNAMIC_CALLBACK_FAILURE,
                    StripeException.create(error),
                )
                GooglePayPaymentDataRequestUpdate(
                    error = mapError(error),
                )
            },
        )
    }

    private fun createRequestUpdateFromCurrentState(
        intermediatePaymentData: GooglePayIntermediatePaymentData,
    ): GooglePayPaymentDataRequestUpdate {
        val state = stateHolder.state
            ?: return GooglePayPaymentDataRequestUpdate(
                error = GooglePayPaymentDataError(
                    reason = GooglePayPaymentDataError.Reason.OTHER_ERROR,
                    message = "Checkout session is not configured.",
                    intent = GooglePayPaymentDataError.Intent.SHIPPING_ADDRESS,
                ),
            )

        return CheckoutGooglePayPaymentDataMapper.createRequestUpdate(
            state = state,
            context = context,
            intermediatePaymentData = intermediatePaymentData,
        )
    }

    private fun mapError(error: Throwable): GooglePayPaymentDataError {
        val message = error.message.orEmpty()
        return when {
            message.contains("allowedShippingCountries", ignoreCase = true) -> {
                GooglePayPaymentDataError(
                    reason = GooglePayPaymentDataError.Reason.SHIPPING_ADDRESS_UNSERVICEABLE,
                    message = message,
                    intent = GooglePayPaymentDataError.Intent.SHIPPING_ADDRESS,
                )
            }
            message.contains("Country is required", ignoreCase = true) ||
                message.contains("Country code", ignoreCase = true) -> {
                GooglePayPaymentDataError(
                    reason = GooglePayPaymentDataError.Reason.SHIPPING_ADDRESS_INVALID,
                    message = message,
                    intent = GooglePayPaymentDataError.Intent.SHIPPING_ADDRESS,
                )
            }
            message.contains("payment flow is presented", ignoreCase = true) ||
                message.contains("not configured", ignoreCase = true) -> {
                GooglePayPaymentDataError(
                    reason = GooglePayPaymentDataError.Reason.OTHER_ERROR,
                    message = message,
                    intent = GooglePayPaymentDataError.Intent.SHIPPING_ADDRESS,
                )
            }
            else -> {
                GooglePayPaymentDataError(
                    reason = GooglePayPaymentDataError.Reason.OTHER_ERROR,
                    message = message.ifEmpty { "Unable to update shipping address." },
                    intent = GooglePayPaymentDataError.Intent.SHIPPING_ADDRESS,
                )
            }
        }
    }
}
