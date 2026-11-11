package com.stripe.android.checkout

import com.stripe.android.checkout.analytics.CheckoutAnalyticsEvent.InvocationSource
import com.stripe.android.checkout.analytics.CheckoutAnalyticsEvent.Operation
import com.stripe.android.checkout.analytics.CheckoutAnalyticsReporter
import com.stripe.android.checkout.analytics.LocalOperation
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionRepository
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import javax.inject.Inject

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutSessionTaxRegionUpdater @Inject constructor(
    private val checkoutSessionRepository: CheckoutSessionRepository,
    private val analyticsReporter: CheckoutAnalyticsReporter,
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
        invocationSource: InvocationSource,
        address: CheckoutElementsController.Address.State?,
    ): Result<CheckoutSessionResponse> {
        val didRequestTaxUpdate = requiresUpdate(
            checkoutSessionResponse = checkoutSessionResponse,
            addressSource = addressSource,
        )
        val params = mapOf(
            "invocation_source" to invocationSource.code,
            "did_request_tax_update" to didRequestTaxUpdate,
        )
        return if (didRequestTaxUpdate) {
            checkoutSessionRepository.measure(
                operation = addressSource.updateOperation(),
                checkoutSessionResponse = checkoutSessionResponse,
                params = params,
            ) {
                checkoutSessionRepository.updateTaxRegion(
                    clientSecret = checkoutSessionResponse.clientSecret,
                    address = address,
                )
            }
        } else {
            Result.success(checkoutSessionResponse).also {
                LocalOperation(
                    operation = addressSource.updateOperation(),
                    params = params,
                    report = analyticsReporter::fireEvent,
                ).succeeded(checkoutSessionResponse)
            }
        }
    }

    private fun CheckoutSessionResponse.TaxAddressSource.updateOperation(): Operation = when (this) {
        CheckoutSessionResponse.TaxAddressSource.SHIPPING -> Operation.UpdateShippingAddress
        CheckoutSessionResponse.TaxAddressSource.BILLING -> Operation.UpdateBillingAddress
    }
}
