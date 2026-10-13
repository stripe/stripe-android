package com.stripe.android.paymentelement.embedded.sheet

import com.stripe.android.checkout.CheckoutController
import com.stripe.android.checkout.CheckoutSessionTaxRegionUpdater
import com.stripe.android.checkout.analytics.CheckoutInvocationSource
import com.stripe.android.checkout.toCheckoutAddress
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
        updateTaxRegion = { response, addressSource, address ->
            taxRegionUpdater.updateServerStateIfNeeded(
                checkoutSessionResponse = response,
                addressSource = addressSource,
                address = address,
                invocationSource = CheckoutInvocationSource.PaymentElement,
            )
        },
    )

    fun prepareUpdate(
        checkoutSessionResponse: CheckoutSessionResponse?,
        selection: PaymentSelection?,
    ): SheetTaxRegionUpdate? {
        val initialResponse = checkoutSessionResponse
            ?.takeIf { it.collectsTaxFromBillingAddress }
            ?: return null
        val address = selection?.billingDetails?.address?.toCheckoutAddress()
            ?: return null

        return {
            updateTaxRegion(
                initialResponse,
                CheckoutSessionResponse.TaxAddressSource.BILLING,
                address,
            )
        }
    }
}
