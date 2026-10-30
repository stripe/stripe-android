package com.stripe.android.checkout

import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdate
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import javax.inject.Inject

@OptIn(CheckoutSessionPreview::class)
internal interface CheckoutGooglePayPaymentDataUpdateAddressBuilder {
    sealed interface Result {
        data object Unavailable : Result

        data class Available(
            val address: CheckoutController.Address.State,
        ) : Result
    }

    fun build(
        paymentDataUpdate: GooglePayPaymentDataUpdate,
        addressSource: CheckoutSessionResponse.TaxAddressSource,
    ): Result
}

@OptIn(CheckoutSessionPreview::class)
internal class DefaultCheckoutGooglePayPaymentDataUpdateAddressBuilder @Inject constructor() :
    CheckoutGooglePayPaymentDataUpdateAddressBuilder {
    override fun build(
        paymentDataUpdate: GooglePayPaymentDataUpdate,
        addressSource: CheckoutSessionResponse.TaxAddressSource,
    ): CheckoutGooglePayPaymentDataUpdateAddressBuilder.Result {
        val paymentAddress = when (addressSource) {
            CheckoutSessionResponse.TaxAddressSource.SHIPPING -> paymentDataUpdate.shippingAddress
            CheckoutSessionResponse.TaxAddressSource.BILLING -> paymentDataUpdate.billingAddress
        } ?: return CheckoutGooglePayPaymentDataUpdateAddressBuilder.Result.Unavailable

        return CheckoutGooglePayPaymentDataUpdateAddressBuilder.Result.Available(
            address = CheckoutController.Address(country = paymentAddress.countryCode)
                .city(paymentAddress.locality.takeIf { it.isNotEmpty() })
                .postalCode(paymentAddress.postalCode.takeIf { it.isNotEmpty() })
                .state(paymentAddress.normalizedAdministrativeArea.takeIf { it.isNotEmpty() })
                .build()
        )
    }
}
