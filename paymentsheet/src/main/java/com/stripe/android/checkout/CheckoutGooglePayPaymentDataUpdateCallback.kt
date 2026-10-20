package com.stripe.android.checkout

import android.content.Context
import android.util.Log
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataError
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdate
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdateCallback
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdateResponse
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.payments.core.analytics.ErrorReporter
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
        Log.d("xkcd", "received callback from trigger: ${paymentDataUpdate.callbackTrigger?.name}")
        val currentState = stateHolder.state ?: run {
            return notConfiguredResponse()
        }

        val countryCode = currentState.checkoutSessionResponse.merchantCountry

        return when (val trigger = paymentDataUpdate.callbackTrigger) {
            GooglePayPaymentDataUpdate.CallbackTrigger.Initialize -> {
                updateAddresses(
                    countryCode = countryCode,
                    currentState = currentState,
                    paymentDataUpdate = paymentDataUpdate,
                    addressSources = listOf(
                        CheckoutSessionResponse.TaxAddressSource.SHIPPING,
                        CheckoutSessionResponse.TaxAddressSource.BILLING,
                    ),
                )
            }
            GooglePayPaymentDataUpdate.CallbackTrigger.ShippingAddress -> {
                updateAddresses(
                    countryCode = countryCode,
                    currentState = currentState,
                    paymentDataUpdate = paymentDataUpdate,
                    addressSources = listOf(CheckoutSessionResponse.TaxAddressSource.SHIPPING),
                )
            }
            GooglePayPaymentDataUpdate.CallbackTrigger.PaymentMethod -> {
                updateAddresses(
                    countryCode = countryCode,
                    currentState = currentState,
                    paymentDataUpdate = paymentDataUpdate,
                    addressSources = listOf(CheckoutSessionResponse.TaxAddressSource.BILLING),
                )
            }
            GooglePayPaymentDataUpdate.CallbackTrigger.ShippingOption,
            GooglePayPaymentDataUpdate.CallbackTrigger.Offer,
            null -> {
                reportUnexpectedCallbackTrigger(
                    if (trigger == GooglePayPaymentDataUpdate.CallbackTrigger.ShippingOption) {
                        VALUE_SHIPPING_OPTION_TRIGGER
                    } else {
                        VALUE_OFFER_TRIGGER
                    }
                )

                mapToResponse(
                    countryCode = countryCode,
                    response = currentState.checkoutSessionResponse,
                    paymentDataUpdate = paymentDataUpdate,
                )
            }
        }
    }

    private suspend fun updateAddresses(
        countryCode: String?,
        currentState: CheckoutControllerState,
        paymentDataUpdate: GooglePayPaymentDataUpdate,
        addressSources: List<CheckoutSessionResponse.TaxAddressSource>,
    ): GooglePayPaymentDataUpdateResponse {
        var response = currentState.checkoutSessionResponse

        for (addressSource in addressSources) {
            val address = when (val result = addressBuilder.build(paymentDataUpdate, addressSource)) {
                is CheckoutGooglePayPaymentDataUpdateAddressBuilder.Result.Unavailable -> continue
                is CheckoutGooglePayPaymentDataUpdateAddressBuilder.Result.Available -> result.address
            }

            Log.d("xkcd", "updating address: $addressSource: in ${address.city}, ${address.country}")

            response = taxRegionUpdater.updateServerStateIfNeeded(
                checkoutSessionResponse = response,
                addressSource = addressSource,
                address = address,
            ).getOrElse { error ->
                return addressUpdateErrorResponse(addressSource, error)
            }
        }

        return mapToResponse(countryCode, response, paymentDataUpdate)
    }

    private fun addressUpdateErrorResponse(
        addressSource: CheckoutSessionResponse.TaxAddressSource,
        error: Throwable,
    ): GooglePayPaymentDataUpdateResponse {
        return GooglePayPaymentDataUpdateResponse(
            newTransactionInfo = null,
            error = GooglePayPaymentDataError(
                reason = when (addressSource) {
                    CheckoutSessionResponse.TaxAddressSource.SHIPPING ->
                        GooglePayPaymentDataError.Reason.ShippingAddressInvalid
                    CheckoutSessionResponse.TaxAddressSource.BILLING ->
                        GooglePayPaymentDataError.Reason.OtherError
                },
                message = error.stripeErrorMessage(context),
                intent = when (addressSource) {
                    CheckoutSessionResponse.TaxAddressSource.SHIPPING ->
                        GooglePayPaymentDataError.Intent.ShippingAddress
                    CheckoutSessionResponse.TaxAddressSource.BILLING ->
                        GooglePayPaymentDataError.Intent.PaymentMethod
                },
            ),
        )
    }

    private fun mapToResponse(
        countryCode: String?,
        response: CheckoutSessionResponse,
        paymentDataUpdate: GooglePayPaymentDataUpdate
    ): GooglePayPaymentDataUpdateResponse {
        return mapper.toResponse(countryCode, response, paymentDataUpdate)
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
    }
}
