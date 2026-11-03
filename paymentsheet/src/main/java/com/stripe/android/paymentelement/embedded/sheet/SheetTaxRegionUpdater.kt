package com.stripe.android.paymentelement.embedded.sheet

import com.stripe.android.checkout.CheckoutController
import com.stripe.android.checkout.CheckoutSessionTaxRegionUpdater
import com.stripe.android.checkout.analytics.CheckoutAnalyticsEvent.InvocationSource
import com.stripe.android.checkout.analytics.CheckoutOperationReporter
import com.stripe.android.checkout.analytics.MeasuredOperation
import com.stripe.android.checkout.analytics.analyticsParams
import com.stripe.android.checkout.analytics.measureAddressUpdate
import com.stripe.android.checkout.analytics.reportAddressUpdateWithoutRequest
import com.stripe.android.checkout.analytics.updateServerStateIfNeeded
import com.stripe.android.checkout.toCheckoutAddress
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.billingDetails
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import javax.inject.Inject

internal typealias SheetTaxRegionUpdate = suspend () -> Result<CheckoutSessionResponse>

@OptIn(CheckoutSessionPreview::class)
internal typealias TaxRegionUpdate = suspend (
    MeasuredOperation,
    CheckoutSessionResponse,
    CheckoutSessionResponse.TaxAddressSource,
    CheckoutController.Address.State?,
) -> Result<CheckoutSessionResponse>

@OptIn(CheckoutSessionPreview::class)
internal class SheetTaxRegionUpdater internal constructor(
    private val updateTaxRegion: TaxRegionUpdate,
    private val operationReporter: CheckoutOperationReporter,
) {
    @Inject
    constructor(
        taxRegionUpdater: CheckoutSessionTaxRegionUpdater,
        operationReporter: CheckoutOperationReporter,
    ) : this(
        updateTaxRegion = { operation, response, addressSource, address ->
            taxRegionUpdater.updateServerStateIfNeeded(operation, response, addressSource, address)
        },
        operationReporter = operationReporter,
    )

    /**
     * Prepares the billing sync for a Checkout selection. Every Checkout sync reports
     * update_billing_address, but only sessions that collect tax from the billing address need an
     * update; otherwise this reports the sync and returns null.
     */
    fun prepareUpdate(
        checkoutSessionResponse: CheckoutSessionResponse?,
        selection: PaymentSelection?,
    ): SheetTaxRegionUpdate? {
        val initialResponse = checkoutSessionResponse ?: return null
        if (!initialResponse.collectsTaxFromBillingAddress) {
            operationReporter.reportAddressUpdateWithoutRequest(
                addressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
                invocationSource = InvocationSource.PaymentElement,
                response = initialResponse,
            )
            return null
        }
        val address = selection?.billingDetails?.address?.toCheckoutAddress()

        return {
            operationReporter.measureAddressUpdate(
                addressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
                params = InvocationSource.PaymentElement.analyticsParams(),
                response = initialResponse,
            ) { operation ->
                updateTaxRegion(
                    operation,
                    initialResponse,
                    CheckoutSessionResponse.TaxAddressSource.BILLING,
                    address,
                )
            }
        }
    }
}
