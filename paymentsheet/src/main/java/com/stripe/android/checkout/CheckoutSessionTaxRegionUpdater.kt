package com.stripe.android.checkout

import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionRepository
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import javax.inject.Inject

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutSessionTaxRegionUpdater @Inject constructor(
    private val checkoutSessionRepository: CheckoutSessionRepository,
) {
    fun requiresUpdate(
        checkoutSessionResponse: CheckoutSessionResponse,
        addressSource: CheckoutSessionResponse.TaxAddressSource,
    ): Boolean {
        return checkoutSessionResponse.automaticTaxEnabled &&
            checkoutSessionResponse.taxAddressSource == addressSource
    }

    suspend fun updateServerStateIfNeeded(
        checkoutSessionResponse: CheckoutSessionResponse,
        addressSource: CheckoutSessionResponse.TaxAddressSource,
        address: CheckoutController.Address.State,
    ): Result<CheckoutSessionResponse> {
        return if (requiresUpdate(checkoutSessionResponse, addressSource)) {
            checkoutSessionRepository.updateTaxRegion(
                sessionId = checkoutSessionResponse.id,
                address = address,
            )
        } else {
            Result.success(checkoutSessionResponse)
        }
    }
}
