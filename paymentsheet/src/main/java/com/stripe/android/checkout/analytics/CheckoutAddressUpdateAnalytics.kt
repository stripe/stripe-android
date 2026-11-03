package com.stripe.android.checkout.analytics

import com.stripe.android.checkout.analytics.CheckoutAnalyticsEvent.Operation
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
    response: CheckoutSessionResponse,
) {
    reportCompleted(
        operation = addressSource.operation,
        params = emptyMap(),
        response = response,
    )
}

private val TaxAddressSource.operation: Operation
    get() = when (this) {
        TaxAddressSource.SHIPPING -> Operation.UpdateShippingAddress
        TaxAddressSource.BILLING -> Operation.UpdateBillingAddress
    }
