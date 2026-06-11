package com.stripe.android.checkout

import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.billingDetails
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse

/**
 * Handles in-sheet checkout session updates that must complete before the sheet can dismiss.
 * Currently supports updating billing address for tax recalculation.
 *
 * Both FlowController and Embedded payment element paths use this to update the session
 * with billing details before proceeding, ensuring the amount (with tax) is consistent
 * before confirmation.
 */
@OptIn(CheckoutSessionPreview::class)
internal class InSheetCheckoutSessionUpdater(
    private val paymentMethodMetadata: PaymentMethodMetadata,
) {
    /**
     * Returns true if the checkout session requires a billing address update for tax calculation
     * before the sheet can dismiss.
     */
    fun requiresTaxUpdate(): Boolean {
        val checkoutMetadata = paymentMethodMetadata.integrationMetadata
            as? IntegrationMetadata.CheckoutSession ?: return false
        val checkout = CheckoutInstances[checkoutMetadata.instancesKey].firstOrNull()
            ?: return false
        return checkout.internalState.checkoutSessionResponse.taxStatus ==
            CheckoutSessionResponse.TaxStatus.REQUIRES_BILLING_ADDRESS
    }

    /**
     * Updates the checkout session with billing details from the payment selection for tax
     * calculation. Should be called before dismissing the sheet when [requiresTaxUpdate] is true.
     *
     * @return [Result.success] if the update succeeded (session now has tax calculated),
     *         [Result.failure] if the update failed.
     */
    suspend fun updateBillingAddressForTax(
        paymentSelection: PaymentSelection,
    ): Result<Unit> {
        val checkoutMetadata = paymentMethodMetadata.integrationMetadata
            as? IntegrationMetadata.CheckoutSession
            ?: return Result.failure(
                IllegalStateException("Not a checkout session integration")
            )

        val checkout = CheckoutInstances[checkoutMetadata.instancesKey].firstOrNull()
            ?: return Result.failure(
                IllegalStateException("No Checkout instance found for key: ${checkoutMetadata.instancesKey}")
            )

        val billingDetails = paymentSelection.billingDetails
            ?: return Result.failure(
                IllegalStateException("Payment selection has no billing details")
            )

        val billingAddress = billingDetails.address
            ?: return Result.failure(
                IllegalStateException("Billing details have no address")
            )

        if (billingAddress.country.isNullOrEmpty()) {
            return Result.failure(
                IllegalStateException("Billing address has no country")
            )
        }

        val address = billingAddress.toCheckoutAddress()

        return checkout.updateBillingAddressFromSheet(
            name = billingDetails.name,
            phoneNumber = billingDetails.phone,
            address = address,
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
