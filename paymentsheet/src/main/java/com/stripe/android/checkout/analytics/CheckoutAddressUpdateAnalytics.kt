package com.stripe.android.checkout.analytics

import com.stripe.android.checkout.CheckoutElementsController
import com.stripe.android.checkout.CheckoutSessionTaxRegionUpdater
import com.stripe.android.checkout.analytics.CheckoutAnalyticsEvent.InvocationSource
import com.stripe.android.checkout.analytics.CheckoutAnalyticsEvent.Operation
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse.TaxAddressSource

/** Measures [update] as the address operation for [addressSource]. */
internal suspend fun CheckoutOperationReporter.measureAddressUpdate(
    addressSource: TaxAddressSource,
    params: Map<String, Any?> = emptyMap(),
    response: CheckoutSessionResponse,
    update: suspend (MeasuredOperation) -> Result<CheckoutSessionResponse>,
): Result<CheckoutSessionResponse> = measure(
    operation = addressSource.operation,
    params = params,
    response = response,
) { operation ->
    update(operation).onSuccess { operation.recordResponse(it) }
}

/** Reports an address sync that needed no tax update. */
internal fun CheckoutOperationReporter.reportAddressUpdateWithoutRequest(
    addressSource: TaxAddressSource,
    invocationSource: InvocationSource,
    response: CheckoutSessionResponse,
) {
    reportCompleted(
        operation = addressSource.operation,
        params = invocationSource.analyticsParams(),
        response = response,
    )
}

/** Sends the tax update when needed and records on [operation] whether it was sent. */
@OptIn(CheckoutSessionPreview::class)
internal suspend fun CheckoutSessionTaxRegionUpdater.updateServerStateIfNeeded(
    operation: MeasuredOperation,
    checkoutSessionResponse: CheckoutSessionResponse,
    addressSource: TaxAddressSource,
    address: CheckoutElementsController.Address.State?,
): Result<CheckoutSessionResponse> {
    if (requiresUpdate(checkoutSessionResponse, addressSource)) {
        operation.putCompletionParam(DID_REQUEST_TAX_UPDATE, true)
    }
    return updateServerStateIfNeeded(checkoutSessionResponse, addressSource, address)
}

internal fun InvocationSource.analyticsParams(): Map<String, Any?> = mapOf("invocation_source" to code)

internal const val DID_REQUEST_TAX_UPDATE = "did_request_tax_update"

private val TaxAddressSource.operation: Operation
    get() = when (this) {
        TaxAddressSource.SHIPPING -> Operation.UpdateShippingAddress
        TaxAddressSource.BILLING -> Operation.UpdateBillingAddress
    }
