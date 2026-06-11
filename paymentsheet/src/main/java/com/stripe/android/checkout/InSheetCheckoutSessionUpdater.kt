package com.stripe.android.checkout

import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.billingDetails
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse

@OptIn(CheckoutSessionPreview::class)
internal class InSheetCheckoutSessionUpdater(
    private val checkout: Checkout?,
) {
    fun requiresUpdate(): Boolean {
        val checkout = checkout ?: return false
        val taxStatus = checkout.internalState.checkoutSessionResponse.taxStatus
        return taxStatus == CheckoutSessionResponse.TaxStatus.REQUIRES_BILLING_ADDRESS ||
            taxStatus == CheckoutSessionResponse.TaxStatus.REQUIRES_SHIPPING_ADDRESS
    }

    suspend fun performUpdate(
        paymentSelection: PaymentSelection,
    ): Result<Unit> {
        val checkout = checkout
            ?: return Result.failure(IllegalStateException("No Checkout instance"))

        return when (checkout.internalState.checkoutSessionResponse.taxStatus) {
            CheckoutSessionResponse.TaxStatus.REQUIRES_BILLING_ADDRESS ->
                updateWithBillingAddress(checkout, paymentSelection)
            CheckoutSessionResponse.TaxStatus.REQUIRES_SHIPPING_ADDRESS ->
                Result.failure(IllegalStateException("Shipping address update not yet supported"))
            else ->
                Result.success(Unit)
        }
    }

    private suspend fun updateWithBillingAddress(
        checkout: Checkout,
        paymentSelection: PaymentSelection,
    ): Result<Unit> {
        val billingDetails = paymentSelection.billingDetails
            ?: return Result.failure(IllegalStateException("Payment selection has no billing details"))

        val billingAddress = billingDetails.address
            ?: return Result.failure(IllegalStateException("Billing details have no address"))

        if (billingAddress.country.isNullOrEmpty()) {
            return Result.failure(IllegalStateException("Billing address has no country"))
        }

        return checkout.updateBillingAddressInternal(
            name = billingDetails.name,
            phoneNumber = billingDetails.phone,
            address = billingAddress.toCheckoutAddress(),
            isInSheetUpdate = true,
        )
    }
}

@OptIn(CheckoutSessionPreview::class)
private fun com.stripe.android.model.Address.toCheckoutAddress(): Address {
    return Address()
        .country(requireNotNull(country))
        .line1(line1)
        .line2(line2)
        .city(city)
        .state(state)
        .postalCode(postalCode)
}
