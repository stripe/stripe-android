package com.stripe.android.checkout

import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdate
import com.stripe.android.paymentelement.CheckoutSessionPreview
import javax.inject.Inject

@OptIn(CheckoutSessionPreview::class)
internal interface CheckoutGooglePayPaymentDataUpdateAddressBuilder {
    sealed interface Result {
        data object Unavailable : Result

        data class Available(
            val address: CheckoutController.Address.State,
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
            ?: return CheckoutGooglePayPaymentDataUpdateAddressBuilder.Result.Unavailable

        return CheckoutGooglePayPaymentDataUpdateAddressBuilder.Result.Available(
            address = CheckoutController.Address()
                .country(shippingAddress.countryCode)
                .city(shippingAddress.locality.takeIf { it.isNotEmpty() })
                .postalCode(shippingAddress.postalCode.takeIf { it.isNotEmpty() })
                .state(shippingAddress.normalizedAdministrativeArea.takeIf { it.isNotEmpty() })
                .build()
        )
    }
}
