package com.stripe.android.paymentelement.embedded.sheet

import com.stripe.android.checkout.CheckoutController
import com.stripe.android.checkout.CheckoutSessionTaxRegionUpdater
import com.stripe.android.checkout.toCheckoutAddress
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.billingDetails
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import javax.inject.Inject

internal typealias SheetTaxRegionUpdate = suspend () -> Result<CheckoutSessionResponse>

@OptIn(CheckoutSessionPreview::class)
internal typealias TaxRegionUpdate = suspend (
    CheckoutSessionResponse,
    CheckoutSessionResponse.TaxAddressSource,
    CheckoutController.Address.State,
) -> Result<CheckoutSessionResponse>

@OptIn(CheckoutSessionPreview::class)
internal class SheetTaxRegionUpdater internal constructor(
    private val updateTaxRegion: TaxRegionUpdate,
) {
    @Inject
    constructor(taxRegionUpdater: CheckoutSessionTaxRegionUpdater) : this(
        updateTaxRegion = taxRegionUpdater::updateServerStateIfNeeded,
    )

    fun prepareUpdate(
        paymentMethodMetadata: PaymentMethodMetadata,
        selection: PaymentSelection?,
    ): SheetTaxRegionUpdate? {
        val checkoutSessionResponse =
            (paymentMethodMetadata.integrationMetadata as? IntegrationMetadata.CheckoutSession)
                ?.checkoutSessionResponse
                ?.takeIf { it.collectsTaxFromBillingAddress }
                ?: return null
        val address = selection?.billingDetails?.address?.toCheckoutAddress()
            ?: return null

        return {
            updateTaxRegion(
                checkoutSessionResponse,
                CheckoutSessionResponse.TaxAddressSource.BILLING,
                address,
            )
        }
    }
}
