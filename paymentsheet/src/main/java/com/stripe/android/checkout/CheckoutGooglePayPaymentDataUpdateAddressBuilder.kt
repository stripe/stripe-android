package com.stripe.android.checkout

import com.stripe.android.googlepaylauncher.GooglePayPaymentDataError
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdate
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdateResponse
import com.stripe.android.paymentelement.CheckoutSessionPreview
import javax.inject.Inject

@OptIn(CheckoutSessionPreview::class)
internal interface CheckoutGooglePayPaymentDataUpdateAddressBuilder {
    sealed interface Result {
        data class Success(
            val address: CheckoutController.Address.State,
        ) : Result

        data class Failed(
            val response: GooglePayPaymentDataUpdateResponse,
        ) : Result
    }

    fun build(paymentDataUpdate: GooglePayPaymentDataUpdate): Result
}

@OptIn(CheckoutSessionPreview::class)
internal class DefaultCheckoutGooglePayPaymentDataUpdateAddressBuilder @Inject constructor() :
    CheckoutGooglePayPaymentDataUpdateAddressBuilder {
    override fun build(
        paymentDataUpdate: GooglePayPaymentDataUpdate
    ): CheckoutGooglePayPaymentDataUpdateAddressBuilder.Result {
        val shippingAddress = paymentDataUpdate.shippingAddress
            ?: return CheckoutGooglePayPaymentDataUpdateAddressBuilder.Result.Failed(
                GooglePayPaymentDataUpdateResponse(
                    newTransactionInfo = null,
                    error = GooglePayPaymentDataError(
                        reason = GooglePayPaymentDataError.Reason.ShippingAddressInvalid,
                        message = "Shipping address is required.",
                        intent = GooglePayPaymentDataError.Intent.ShippingAddress,
                    ),
                )
            )

        if (shippingAddress.countryCode.isBlank()) {
            return CheckoutGooglePayPaymentDataUpdateAddressBuilder.Result.Failed(
                GooglePayPaymentDataUpdateResponse(
                    newTransactionInfo = null,
                    error = GooglePayPaymentDataError(
                        reason = GooglePayPaymentDataError.Reason.ShippingAddressInvalid,
                        message = "Country is required.",
                        intent = GooglePayPaymentDataError.Intent.ShippingAddress,
                    ),
                )
            )
        }

        return CheckoutGooglePayPaymentDataUpdateAddressBuilder.Result.Success(
            address = CheckoutController.Address()
                .country(shippingAddress.countryCode)
                .city(shippingAddress.locality.takeIf { it.isNotEmpty() })
                .postalCode(shippingAddress.postalCode.takeIf { it.isNotEmpty() })
                .state(
                    shippingAddress.iso3166AdministrativeArea?.takeIf { it.isNotEmpty() }
                        ?: shippingAddress.administrativeArea.takeIf { it.isNotEmpty() },
                )
                .build()
        )
    }
}
