package com.stripe.android.checkout

import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionRepository
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import javax.inject.Inject

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutSessionTaxRegionUpdater @Inject constructor(
    private val checkoutSessionRepository: CheckoutSessionRepository,
) {
    suspend fun updateServerStateIfNeeded(
        checkoutSessionResponse: CheckoutSessionResponse,
        addressSource: CheckoutSessionResponse.TaxAddressSource,
        address: CheckoutController.Address.State,
    ): Result<CheckoutSessionResponse> {
        val shouldUpdate = checkoutSessionResponse.automaticTaxEnabled &&
            checkoutSessionResponse.taxAddressSource == addressSource

        return if (shouldUpdate) {
            checkoutSessionRepository.updateTaxRegion(
                sessionId = checkoutSessionResponse.id,
                address = address,
            )
        } else {
            Result.success(checkoutSessionResponse)
        }
    }
}
