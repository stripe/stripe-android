package com.stripe.android.checkout.gpay

import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.checkout.CheckoutControllerState
import com.stripe.android.paymentelement.CheckoutSessionPreview

@OptIn(CheckoutSessionPreview::class)
internal data class CheckoutGooglePayShippingConfiguration(
    val shippingAddressParameters: GooglePayJsonFactory.ShippingAddressParameters,
    val shippingOptionsParameters: GooglePayJsonFactory.ShippingOptionParameters?,
)

@OptIn(CheckoutSessionPreview::class)
internal object CheckoutGooglePayShippingConfigurationFactory {
    fun create(state: CheckoutControllerState): CheckoutGooglePayShippingConfiguration? {
        if (!isShippingAddressCollectionEnabled(state)) {
            return null
        }

        return CheckoutGooglePayShippingConfiguration(
            shippingAddressParameters = GooglePayJsonFactory.ShippingAddressParameters(
                isRequired = true,
                allowedCountryCodes = state.checkoutSessionResponse.allowedShippingCountries.orEmpty().toSet(),
            ),
            shippingOptionsParameters = CheckoutGooglePayPaymentDataMapper.createShippingOptionParameters(
                response = state.checkoutSessionResponse,
            ),
        )
    }

    private fun isShippingAddressCollectionEnabled(state: CheckoutControllerState): Boolean {
        return state.configuration.expressCheckoutElementConfiguration.shippingAddressRequired ||
            !state.checkoutSessionResponse.allowedShippingCountries.isNullOrEmpty()
    }
}
