@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CheckoutElementsController.Session
import com.stripe.android.model.parsers.PaymentIntentJsonParser
import com.stripe.android.paymentsheet.model.CheckoutSessionClientSecret
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse.TaxAddressSource
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseJsonParser
import org.json.JSONObject
import org.junit.Test
import java.io.File
import java.util.Locale

class AutomaticTaxLiveResponseBashTest {
    @Test
    fun `captured automatic billing changes map to correct tax states and confirmed totals`() {
        assertSession("billing-auto-init", TaxAddressSource.BILLING, Session.Tax.Status.RequiresBillingAddress, 0)
        assertSession("billing-auto-0-sf", TaxAddressSource.BILLING, Session.Tax.Status.Ready, 1035)
        assertSession("billing-auto-1-oregon", TaxAddressSource.BILLING, Session.Tax.Status.Ready, 0)
        assertSession("billing-auto-2-canada", TaxAddressSource.BILLING, Session.Tax.Status.Ready, 0)
        assertSession("billing-auto-3-france", TaxAddressSource.BILLING, Session.Tax.Status.Ready, 0)
        assertSession("billing-auto-4-clear", TaxAddressSource.BILLING, Session.Tax.Status.RequiresBillingAddress, 0)
        assertSession("billing-auto-5-sf", TaxAddressSource.BILLING, Session.Tax.Status.Ready, 1035)
        assertConfirmed("billing-auto", TaxAddressSource.BILLING, 1035)
    }

    @Test
    fun `captured required billing can clear and restore before confirmation`() {
        assertThat(parse("billing-required-init").requiresBillingAddress).isTrue()
        assertSession("billing-required-init", TaxAddressSource.BILLING, Session.Tax.Status.RequiresBillingAddress, 0)
        assertSession("billing-required-0-sf", TaxAddressSource.BILLING, Session.Tax.Status.Ready, 1035)
        assertSession("billing-required-1-clear", TaxAddressSource.BILLING, Session.Tax.Status.RequiresBillingAddress, 0)
        assertSession("billing-required-2-oregon", TaxAddressSource.BILLING, Session.Tax.Status.Ready, 0)
        assertConfirmed("billing-required", TaxAddressSource.BILLING, 0)
    }

    @Test
    fun `captured shipping tax retains its source despite different confirmation billing`() {
        val initial = parse("shipping-init")
        assertThat(initial.requiresShippingAddress).isTrue()
        assertThat(initial.requiresBillingAddress).isTrue()
        assertSession("shipping-init", TaxAddressSource.SHIPPING, Session.Tax.Status.RequiresShippingAddress, 0)
        assertSession("shipping-0-sf", TaxAddressSource.SHIPPING, Session.Tax.Status.Ready, 1035)
        assertSession("shipping-1-canada", TaxAddressSource.SHIPPING, Session.Tax.Status.Ready, 0)
        assertSession("shipping-2-clear", TaxAddressSource.SHIPPING, Session.Tax.Status.RequiresShippingAddress, 0)
        assertSession("shipping-3-oregon", TaxAddressSource.SHIPPING, Session.Tax.Status.Ready, 0)
        assertConfirmed("shipping", TaxAddressSource.SHIPPING, 0)
    }

    @Test
    fun `captured customer update policy selects whether billing controls tax`() {
        assertSession("customer-source-init", null, Session.Tax.Status.Ready, 1035)
        assertThat(parse("customer-source-init").collectsTaxFromBillingAddress).isFalse()
        assertSession("customer-billing-source-init", TaxAddressSource.BILLING, Session.Tax.Status.RequiresBillingAddress, 0)
        assertSession("customer-billing-source-oregon", TaxAddressSource.BILLING, Session.Tax.Status.Ready, 0)
    }

    private fun assertSession(name: String, source: TaxAddressSource?, status: Session.Tax.Status, tax: Long) {
        val response = parse(name)
        assertThat(response.automaticTaxEnabled).isTrue()
        assertThat(response.taxAddressSource).isEqualTo(source)
        assertThat(response.collectsTaxFromBillingAddress).isEqualTo(source == TaxAddressSource.BILLING)
        assertThat(response.amount).isEqualTo(12000 + tax)
        val session = response.session()
        assertThat(session.tax?.status).isEqualTo(status)
        assertThat(session.totals.subtotal.minorUnitsAmount).isEqualTo(12000)
        assertThat(session.totals.taxExclusive.minorUnitsAmount).isEqualTo(tax)
        assertThat(session.totals.total.minorUnitsAmount).isEqualTo(12000 + tax)
        assertThat(session.totals.total.amount).isEqualTo(if (tax == 1035L) "$130.35" else "$120.00")
        assertThat(session.orderSummaryItems.flatMap { (it as Session.OrderSummaryItem.OneTimePrice).items }
            .sumOf { it.amountDetails.total.minorUnitsAmount }).isEqualTo(12000 + tax)
    }

    private fun assertConfirmed(name: String, source: TaxAddressSource, tax: Long) {
        assertSession("$name-confirm", source, Session.Tax.Status.Ready, tax)
        val response = parse("$name-confirm")
        val intent = requireNotNull(PaymentIntentJsonParser().parse(read("$name-payment-intent")))
        assertThat(intent.amount).isEqualTo(12000 + tax)
        val session = response.withIntent(intent).session()
        val complete = session.status as Session.Status.Complete
        assertThat(complete.paymentStatus).isInstanceOf(Session.Status.PaymentStatus.Paid::class.java)
        assertThat(session.totals.total.minorUnitsAmount).isEqualTo(intent.amount)
    }

    private fun CheckoutSessionResponse.session() = asCheckoutSession(
        collectedEmail = null,
        collectedShippingName = null,
        collectedShippingAddress = null,
        flagImages = null,
        paymentOption = null,
        availableExpressButtonTypes = emptyList(),
        locale = Locale.US,
    )

    private fun parse(name: String): CheckoutSessionResponse {
        val json = read(name)
        return requireNotNull(CheckoutSessionResponseJsonParser(
            CheckoutSessionClientSecret("${json.getString("id")}_secret_redacted")
        ).parse(json))
    }

    private fun read(name: String): JSONObject {
        val directory = generateSequence(File(".").absoluteFile) { it.parentFile }
            .map { File(it, "bug-bash/automatic-tax") }.first { it.isDirectory }
        return JSONObject(File(directory, "$name.json").readText())
    }
}
