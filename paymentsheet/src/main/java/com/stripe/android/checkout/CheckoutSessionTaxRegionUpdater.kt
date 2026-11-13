package com.stripe.android.checkout

import com.stripe.android.checkout.analytics.CheckoutAnalyticsEvent.InvocationSource
import com.stripe.android.checkout.analytics.CheckoutAnalyticsEvent.Operation
import com.stripe.android.checkout.analytics.CheckoutOperationMeasurer
import com.stripe.android.checkout.analytics.LocalOperation
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionRepository
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import javax.inject.Inject

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutSessionTaxRegionUpdater @Inject constructor(
    private val checkoutSessionRepository: CheckoutSessionRepository,
    private val operationMeasurer: CheckoutOperationMeasurer,
    private val localOperationFactory: LocalOperation.Factory,
) {
    fun requiresUpdate(
        checkoutSessionResponse: CheckoutSessionResponse,
        addressSource: CheckoutSessionResponse.TaxAddressSource,
    ): Boolean {
        return checkoutSessionResponse.automaticTaxEnabled &&
            checkoutSessionResponse.taxAddressSource == addressSource
    }

    fun analyticsParams(
        invocationSource: InvocationSource,
        didRequestTaxUpdate: Boolean,
    ): Map<String, Any?> = mapOf(
        "invocation_source" to invocationSource.code,
        "did_request_tax_update" to didRequestTaxUpdate,
    )

    /** Reports the update as its own address operation; see [updateTaxRegionIfNeeded] to skip reporting. */
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
        val params = analyticsParams(
            invocationSource = invocationSource,
            didRequestTaxUpdate = didRequestTaxUpdate,
        )
        return if (didRequestTaxUpdate) {
            operationMeasurer.measure(
                operation = addressSource.updateOperation(),
                checkoutSessionResponse = checkoutSessionResponse,
                params = params,
            ) {
                updateTaxRegionIfNeeded(
                    checkoutSessionResponse = checkoutSessionResponse,
                    addressSource = addressSource,
                    address = address,
                )
            }
        } else {
            Result.success(checkoutSessionResponse).also {
                localOperationFactory.create(
                    operation = addressSource.updateOperation(),
                    params = params,
                ).succeeded(checkoutSessionResponse)
            }
        }
    }

    suspend fun updateTaxRegionIfNeeded(
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
