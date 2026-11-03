package com.stripe.android.checkout

import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionRepository
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
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
        address: CheckoutElementsController.Address.State?,
    ): TaxRegionUpdateOutcome {
        if (!requiresUpdate(checkoutSessionResponse, addressSource)) {
            return TaxRegionUpdateOutcome(Result.success(checkoutSessionResponse), requestAttempted = false)
        }
        val result = try {
            checkoutSessionRepository.updateTaxRegion(
                sessionId = checkoutSessionResponse.id,
                clientSecret = checkoutSessionResponse.clientSecret,
                address = address,
            ).also { currentCoroutineContext().ensureActive() }
        } catch (error: CancellationException) {
            // Preserve the request fact until the owning flow records it and propagates cancellation.
            Result.failure(error)
        }
        return TaxRegionUpdateOutcome(result, requestAttempted = true)
    }
}
