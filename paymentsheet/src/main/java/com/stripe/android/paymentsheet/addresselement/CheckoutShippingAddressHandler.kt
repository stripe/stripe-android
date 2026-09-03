package com.stripe.android.paymentsheet.addresselement

import com.stripe.android.checkout.CheckoutController
import com.stripe.android.checkout.CheckoutControllerReferences
import com.stripe.android.paymentelement.CheckoutSessionPreview
import javax.inject.Inject

internal interface CheckoutShippingAddressHandler {
    suspend fun update(
        controllerInstanceId: String,
        addressDetails: AddressDetails,
    ): Result<Unit>
}

@OptIn(CheckoutSessionPreview::class)
internal class DefaultCheckoutShippingAddressHandler @Inject constructor() : CheckoutShippingAddressHandler {
    override suspend fun update(
        controllerInstanceId: String,
        addressDetails: AddressDetails,
    ): Result<Unit> {
        val controller = CheckoutControllerReferences[controllerInstanceId]
            ?: return Result.failure(IllegalStateException("Checkout controller is unavailable."))
        val address = CheckoutController.Address()
            .city(addressDetails.address?.city)
            .country(requireNotNull(addressDetails.address?.country))
            .line1(addressDetails.address?.line1)
            .line2(addressDetails.address?.line2)
            .postalCode(addressDetails.address?.postalCode)
            .state(addressDetails.address?.state)

        return controller.updateShippingAddress(addressDetails.name, address)
    }
}
