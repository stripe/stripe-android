package com.stripe.android.checkout

import com.stripe.android.checkout.analytics.CheckoutAnalyticsOperation
import com.stripe.android.checkout.analytics.CheckoutEventReporter
import com.stripe.android.checkout.analytics.CheckoutInvocationSource
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionRepository
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutSessionTaxRegionUpdater @Inject constructor(
    private val checkoutSessionRepository: CheckoutSessionRepository,
    private val eventReporter: CheckoutEventReporter,
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
        invocationSource: CheckoutInvocationSource?,
    ): Result<CheckoutSessionResponse> {
        val didRequestTaxUpdate = requiresUpdate(checkoutSessionResponse, addressSource)
        val additionalParams = buildMap<String, Any?> {
            invocationSource?.value?.let { put(FIELD_INVOCATION_SOURCE, it) }
            put(FIELD_DID_REQUEST_TAX_UPDATE, didRequestTaxUpdate)
        }
        val span = invocationSource?.let {
            eventReporter.start(
                operation = addressSource.toAnalyticsOperation(),
                response = checkoutSessionResponse,
                additionalParams = additionalParams,
            )
        }

        val result = if (didRequestTaxUpdate) {
            checkoutSessionRepository.updateTaxRegion(
                sessionId = checkoutSessionResponse.id,
                address = address,
            )
        } else {
            Result.success(checkoutSessionResponse)
        }

        val error = result.exceptionOrNull()
        if (error == null) {
            val response = result.getOrThrow()
            span?.succeeded(
                response = response,
                requestId = response.requestId.takeIf { didRequestTaxUpdate },
                additionalParams = additionalParams,
            )
        } else if (error !is CancellationException) {
            span?.failed(
                error = error,
                response = checkoutSessionResponse,
                additionalParams = additionalParams,
            )
        }

        return result
    }

    private fun CheckoutSessionResponse.TaxAddressSource.toAnalyticsOperation(): CheckoutAnalyticsOperation {
        return when (this) {
            CheckoutSessionResponse.TaxAddressSource.BILLING -> CheckoutAnalyticsOperation.UpdateBillingAddress
            CheckoutSessionResponse.TaxAddressSource.SHIPPING -> CheckoutAnalyticsOperation.UpdateShippingAddress
        }
    }

    private companion object {
        const val FIELD_DID_REQUEST_TAX_UPDATE = "did_request_tax_update"
        const val FIELD_INVOCATION_SOURCE = "invocation_source"
    }
}
