package com.stripe.android.paymentelement.embedded.sheet

import com.stripe.android.checkout.CheckoutSessionTaxRegionUpdater
import com.stripe.android.checkout.toCheckoutAddress
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.billingDetails
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse

@OptIn(CheckoutSessionPreview::class)
internal class SheetTaxRegionUpdater(
    private val checkoutSessionResponse: CheckoutSessionResponse,
    private val taxRegionUpdater: CheckoutSessionTaxRegionUpdater,
) {
    suspend fun update(selection: PaymentSelection?): Result<CheckoutSessionResponse?> {
        val address = selection?.billingDetails?.address?.toCheckoutAddress()
            ?: return Result.success(null)

        return taxRegionUpdater.updateServerStateIfNeeded(
            checkoutSessionResponse = checkoutSessionResponse,
            addressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
            address = address,
        )
    }
}
