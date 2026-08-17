package com.stripe.android.checkout

import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataError
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdate
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdateCallback
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdateResponse
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import javax.inject.Inject

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutGooglePayPaymentDataUpdateCallback @Inject constructor(
    private val taxRegionUpdater: CheckoutSessionTaxRegionUpdater,
    private val stateHolder: CheckoutControllerStateHolder,
    private val addressBuilder: CheckoutGooglePayPaymentDataUpdateAddressBuilder,
    private val mapper: CheckoutGooglePayPaymentDataUpdateMapper,
) : GooglePayPaymentDataUpdateCallback {
    override suspend fun onPaymentDataChanged(
        paymentDataUpdate: GooglePayPaymentDataUpdate,
    ): GooglePayPaymentDataUpdateResponse {
        val currentState = stateHolder.state ?: return GooglePayPaymentDataUpdateResponse(
            newTransactionInfo = null,
            error = GooglePayPaymentDataError(
                reason = GooglePayPaymentDataError.Reason.OtherError,
                message = "Checkout sessions not configured",
                intent = GooglePayPaymentDataError.Intent.ShippingAddress,
            )
        )

        return when (paymentDataUpdate.callbackTrigger) {
            GooglePayPaymentDataUpdate.CallbackTrigger.Initialize,
            GooglePayPaymentDataUpdate.CallbackTrigger.ShippingAddress -> {
                updateShippingAddress(currentState, paymentDataUpdate)
            }
            GooglePayPaymentDataUpdate.CallbackTrigger.ShippingOption,
            GooglePayPaymentDataUpdate.CallbackTrigger.Offer -> {
                mapToResponse(
                    configuration = currentState.commonConfiguration,
                    response = currentState.checkoutSessionResponse,
                    paymentDataUpdate = paymentDataUpdate,
                )
            }
        }
    }

    private suspend fun updateShippingAddress(
        currentState: CheckoutControllerState,
        paymentDataUpdate: GooglePayPaymentDataUpdate,
    ): GooglePayPaymentDataUpdateResponse {
        val address = when (val result = addressBuilder.build(paymentDataUpdate)) {
            is CheckoutGooglePayPaymentDataUpdateAddressBuilder.Result.Failed -> {
                return result.response
            }
            is CheckoutGooglePayPaymentDataUpdateAddressBuilder.Result.Success -> {
                result.address
            }
        }

        return taxRegionUpdater.updateServerStateIfNeeded(
            checkoutSessionResponse = currentState.checkoutSessionResponse,
            addressSource = CheckoutSessionResponse.TaxAddressSource.SHIPPING,
            address = address,
        ).fold(
            onSuccess = { response ->
                mapToResponse(currentState.commonConfiguration, response, paymentDataUpdate)
            },
            onFailure = { error ->
                GooglePayPaymentDataUpdateResponse(
                    newTransactionInfo = null,
                    error = GooglePayPaymentDataError(
                        reason = GooglePayPaymentDataError.Reason.ShippingAddressInvalid,
                        message = error.message ?: "Unable to update shpping address",
                        intent = GooglePayPaymentDataError.Intent.ShippingAddress,
                    ),
                )
            },
        )
    }

    private fun mapToResponse(
        configuration: CommonConfiguration,
        response: CheckoutSessionResponse,
        paymentDataUpdate: GooglePayPaymentDataUpdate
    ): GooglePayPaymentDataUpdateResponse {
        return mapper.toResponse(configuration, response, paymentDataUpdate)
    }
}
