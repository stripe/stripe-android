@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CheckoutElementsController.Session
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.parsers.PaymentIntentJsonParser
import com.stripe.android.paymentsheet.model.CheckoutSessionClientSecret
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseJsonParser
import org.json.JSONObject
import org.junit.Test
import java.io.File
import java.util.Locale

class AdaptivePricingLiveResponseBashTest {
    @Test
    fun `captured France round trip retains EUR totals and USD integration currency`() {
        assertSession("france-local-init", "eur", 11251, "€112.51", 100)
        assertSession("france-local-update-usd", "usd", 12000, "$120.00", 100)
        assertSession("france-local-update-local", "eur", 11251, "€112.51", 100)
        assertConfirmedSession("france-local", "eur", 11251, "€112.51", 100)
    }

    @Test
    fun `captured Japan round trip formats JPY without decimal division`() {
        assertSession("japan-local-init", "jpy", 19951, "¥19,951", 1)
        assertSession("japan-local-update-usd", "usd", 12000, "$120.00", 100)
        assertSession("japan-local-update-local", "jpy", 19951, "¥19,951", 1)
        assertConfirmedSession("japan-local", "jpy", 19951, "¥19,951", 1)
    }

    @Test
    fun `captured USD payment after Japanese toggles keeps USD total`() {
        assertSession("japan-usd-before-confirm", "usd", 12000, "$120.00", 100)
        assertConfirmedSession("japan-usd", "usd", 12000, "$120.00", 100)
    }

    @Test
    fun `captured disabled adaptive pricing has no selector or presentment details`() {
        val response = parse("france-disabled-init")
        val session = response.session()
        assertThat(response.currency).isEqualTo("usd")
        assertThat(session.currency).isEqualTo("usd")
        assertThat(session.totals.total.minorUnitsAmount).isEqualTo(12000)
        assertThat(session.totals.total.amount).isEqualTo("$120.00")
        assertThat(session.presentmentDetails).isNull()
        assertThat(session.currencySelectorOptions).isNull()
    }

    private fun assertSession(
        name: String,
        currency: String,
        total: Long,
        display: String,
        divisor: Int,
    ) {
        val response = parse(name)
        val session = response.session()
        assertThat(response.currency).isEqualTo(currency)
        assertThat(response.amount).isEqualTo(total)
        assertThat((response.elementsSession?.stripeIntent as PaymentIntent).currency).isEqualTo(currency)
        assertAmounts(session, currency, total, display, divisor)
        assertThat(session.currencySelectorOptions?.selectedCode).isEqualTo(currency.uppercase())
        val options = requireNotNull(session.currencySelectorOptions)
        val selected = listOf(options.first, options.second).single { it.code == currency.uppercase() }
        assertThat(selected.formattedAmount).isEqualTo(display)
        val itemTotals = session.orderSummaryItems.flatMap {
            (it as Session.OrderSummaryItem.OneTimePrice).items
        }.sumOf { it.amountDetails.total.minorUnitsAmount }
        assertThat(itemTotals).isEqualTo(total)
    }

    private fun assertConfirmedSession(
        name: String,
        currency: String,
        total: Long,
        display: String,
        divisor: Int,
    ) {
        val response = parse("$name-confirm")
        val intent = requireNotNull(PaymentIntentJsonParser().parse(read("$name-payment-intent")))
        assertThat(intent.currency).isEqualTo("usd")
        assertThat(intent.amount).isEqualTo(12000)
        val session = response.withIntent(intent).session()
        assertAmounts(session, currency, total, display, divisor)
        val status = session.status as Session.Status.Complete
        assertThat(status.paymentStatus).isInstanceOf(Session.Status.PaymentStatus.Paid::class.java)
    }

    private fun assertAmounts(
        session: Session,
        currency: String,
        total: Long,
        display: String,
        divisor: Int,
    ) {
        assertThat(session.currency).isEqualTo("usd")
        assertThat(session.presentmentDetails?.presentmentCurrency).isEqualTo(currency)
        assertThat(session.totals.total.minorUnitsAmount).isEqualTo(total)
        assertThat(session.totals.total.amount).isEqualTo(display)
        assertThat(session.minorUnitsAmountDivisor).isEqualTo(divisor)
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
        return requireNotNull(
            CheckoutSessionResponseJsonParser(
                CheckoutSessionClientSecret("${json.getString("id")}_secret_redacted")
            ).parse(json)
        ) { "SDK could not parse captured response $name" }
    }

    private fun read(name: String): JSONObject {
        val directory = generateSequence(File(".").absoluteFile) { it.parentFile }
            .map { File(it, "bug-bash/adaptive-pricing") }
            .first { it.isDirectory }
        return JSONObject(File(directory, "$name.json").readText())
    }
}
