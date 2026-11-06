package com.stripe.android.checkout

import com.stripe.android.checkout.analytics.CheckoutAnalyticsEvent.Operation
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

    suspend fun updateAddress(
        checkoutSessionResponse: CheckoutSessionResponse,
        addressSource: CheckoutSessionResponse.TaxAddressSource,
        invocationSource: String,
        address: CheckoutElementsController.Address.State?,
    ): Result<CheckoutSessionResponse> = checkoutSessionRepository.measure(
        operation = addressSource.updateOperation(),
        checkoutSessionResponse = checkoutSessionResponse,
        params = mapOf(
            "invocation_source" to invocationSource,
            "did_request_tax_update" to requiresUpdate(
                checkoutSessionResponse = checkoutSessionResponse,
                addressSource = addressSource,
            ),
        ),
    ) {
        updateServerStateIfNeeded(
            checkoutSessionResponse = checkoutSessionResponse,
            addressSource = addressSource,
            address = address,
        )
    }

    suspend fun updateServerStateIfNeeded(
        checkoutSessionResponse: CheckoutSessionResponse,
        addressSource: CheckoutSessionResponse.TaxAddressSource,
        address: CheckoutElementsController.Address.State?,
    ): Result<CheckoutSessionResponse> {
        return if (requiresUpdate(checkoutSessionResponse, addressSource)) {
            checkoutSessionRepository.updateTaxRegion(
                clientSecret = checkoutSessionResponse.clientSecret,
                address = address,
            )
        } else {
            Result.success(checkoutSessionResponse)
        }
    }

    private fun CheckoutSessionResponse.TaxAddressSource.updateOperation(): Operation = when (this) {
        CheckoutSessionResponse.TaxAddressSource.SHIPPING -> Operation.UpdateShippingAddress
        CheckoutSessionResponse.TaxAddressSource.BILLING -> Operation.UpdateBillingAddress
    }
}
