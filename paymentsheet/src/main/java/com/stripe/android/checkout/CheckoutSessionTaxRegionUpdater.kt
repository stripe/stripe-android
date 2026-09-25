package com.stripe.android.checkout

import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.repositories.CheckoutSessionRepository
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import javax.inject.Inject

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutSessionTaxRegionUpdater @Inject constructor(
    private val checkoutSessionRepository: CheckoutSessionRepository,
    private val errorReporter: ErrorReporter,
) {
    suspend fun updateServerStateIfNeeded(
        checkoutSessionResponse: CheckoutSessionResponse,
        addressSource: CheckoutSessionResponse.TaxAddressSource,
        address: CheckoutController.Address.State?,
    ): Result<CheckoutSessionResponse> {
        val requiresUpdate = checkoutSessionResponse.automaticTaxEnabled &&
            checkoutSessionResponse.taxAddressSource == addressSource
        if (!requiresUpdate) {
            return Result.success(checkoutSessionResponse)
        }
        if (address == null) {
            // Callers only offer addresses that satisfy the tax source, so a missing one is a bug.
            errorReporter.report(
                errorEvent = ErrorReporter.UnexpectedErrorEvent.CHECKOUT_TAX_REGION_UPDATE_MISSING_ADDRESS,
                additionalNonPiiParams = mapOf("address_source" to addressSource.name),
            )
            return Result.success(checkoutSessionResponse)
        }
        return checkoutSessionRepository.updateTaxRegion(
            sessionId = checkoutSessionResponse.id,
            address = address,
        )
    }
}
