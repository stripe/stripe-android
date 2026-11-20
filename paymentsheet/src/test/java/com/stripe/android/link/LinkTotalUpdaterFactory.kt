@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.link

import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse

internal object LinkTotalUpdaterFactory {
    /** Creates an updater for a total that doesn't depend on the billing address. */
    fun disabled(): LinkTotalUpdater {
        return LinkTotalUpdater(
            initialCheckoutSessionResponse = null,
            updateTaxRegion = { response, _ -> Result.success(response) },
        )
    }

    fun create(
        initialCheckoutSessionResponse: CheckoutSessionResponse,
        updateTaxRegion: LinkTaxRegionUpdate,
    ): LinkTotalUpdater {
        return LinkTotalUpdater(
            initialCheckoutSessionResponse = initialCheckoutSessionResponse,
            updateTaxRegion = updateTaxRegion,
        )
    }
}
