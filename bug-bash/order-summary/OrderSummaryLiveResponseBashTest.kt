@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CheckoutElementsController.Session
import com.stripe.android.model.parsers.PaymentIntentJsonParser
import com.stripe.android.paymentsheet.model.CheckoutSessionClientSecret
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseJsonParser
import org.json.JSONObject
import org.junit.Test
import java.io.File
import java.math.BigDecimal
import java.util.Locale

class OrderSummaryLiveResponseBashTest {
    @Test
    fun `details preserve quantities labels images decimal precision and confirmed line totals`() {
        val session = parse("details-init").session()
        val group = session.orderSummaryItems.single() as Session.OrderSummaryItem.OneTimePrice
        val shirt = group.items[0]
        assertThat(group.id).isEqualTo(group.key)
        assertThat(shirt.id).isEqualTo(shirt.key)
        assertThat(shirt.displayName).isEqualTo("Café shirt — blue & white 100%")
        assertThat(shirt.images).containsExactly("https://stripe.com/img/v3/home/twitter.png")
        assertThat(shirt.unitLabel).isEqualTo("shirt")
        assertThat(shirt.quantity).isEqualTo(3)
        assertThat(shirt.unitAmount).isEqualTo(Session.Amount("$12.99", 1299L))
        assertThat(shirt.amountDetails.total).isEqualTo(Session.Amount("$38.97", 3897L))
        assertThat(shirt.adjustableQuantity?.enabled).isTrue()
        assertThat(shirt.adjustableQuantity?.minimum).isEqualTo(0)
        assertThat(shirt.adjustableQuantity?.maximum).isEqualTo(12)
        val ribbon = group.items[1]
        assertThat(ribbon.quantity).isEqualTo(100)
        assertThat(ribbon.unitLabel).isEqualTo("meter")
        assertThat(ribbon.unitAmountDecimal?.minorUnitsAmount).isEqualTo(BigDecimal("12.345678901234"))
        assertThat(ribbon.unitAmountDecimal?.amount).isEqualTo("$0.12345678901234")
        assertThat(ribbon.amountDetails.total).isEqualTo(Session.Amount("$12.35", 1235L))
        assertThat(ribbon.adjustableQuantity).isNull()
        val gift = group.items[2]
        assertThat(gift.quantity).isEqualTo(1)
        assertThat(gift.unitAmount).isEqualTo(Session.Amount("$0.00", 0L))
        assertThat(gift.amountDetails.total).isEqualTo(Session.Amount("$0.00", 0L))
        assertThat(gift.images).isEmpty()
        assertThat(gift.unitLabel).isNull()
        assertThat(session.totals.total).isEqualTo(Session.Amount("$51.32", 5132L))
        assertThat(parse("details-retrieve").session().orderSummaryItems).isEqualTo(session.orderSummaryItems)
        assertConfirmed("details", session)
    }

    @Test
    fun `inclusive tax preserves total while included tax updates and survives confirmation`() {
        val before = parse("inclusive-tax-init").session()
        assertThat(before.tax?.status).isEqualTo(Session.Tax.Status.RequiresShippingAddress)
        assertThat(before.totals.taxInclusive.minorUnitsAmount).isEqualTo(0L)
        val california = parse("inclusive-tax-sf").session()
        assertThat(california.tax?.status).isEqualTo(Session.Tax.Status.Ready)
        assertThat(california.totals.subtotal).isEqualTo(Session.Amount("$45.00", 4500L))
        assertThat(california.totals.total).isEqualTo(Session.Amount("$45.00", 4500L))
        assertThat(california.totals.taxInclusive).isEqualTo(Session.Amount("$3.57", 357L))
        assertThat(california.totals.taxExclusive.minorUnitsAmount).isEqualTo(0L)
        val tax = requireNotNull(california.taxAmounts).single()
        assertThat(tax.inclusive).isTrue()
        assertThat(tax.displayName).isEqualTo("Sales Tax")
        assertThat(tax.percentage).isEqualTo(8.625)
        assertThat(tax.minorUnitsAmount).isEqualTo(357L)
        val items = california.items()
        assertThat(items.map { it.amountDetails.taxInclusive.minorUnitsAmount }).containsExactly(159L, 198L).inOrder()
        assertThat(items.map { it.amountDetails.total.minorUnitsAmount }).containsExactly(2000L, 2500L).inOrder()
        assertThat(items.flatMap { requireNotNull(it.amountDetails.taxAmounts) }.all { it.inclusive }).isTrue()
        val oregon = parse("inclusive-tax-or").session()
        assertThat(oregon.totals.total).isEqualTo(california.totals.total)
        assertThat(oregon.totals.taxInclusive.minorUnitsAmount).isEqualTo(0L)
        assertThat(oregon.items().map { it.key }).containsExactlyElementsIn(items.map { it.key }).inOrder()
        val restored = parse("inclusive-tax-sf-restored").session()
        assertThat(restored.orderSummaryItems).isEqualTo(california.orderSummaryItems)
        assertThat(restored.totals).isEqualTo(california.totals)
        assertConfirmed("inclusive-tax", restored)
    }

    @Test
    fun `ISK uses Stripe divisor through line totals and confirmation`() {
        val session = parse("isk-init").session()
        assertThat(session.currency).isEqualTo("isk")
        assertThat(session.presentmentDetails).isNull()
        assertThat(session.minorUnitsAmountDivisor).isEqualTo(100)
        assertThat(session.totals.total.minorUnitsAmount).isEqualTo(29600L)
        assertThat(session.totals.total.amount).isEqualTo("ISK296.00")
        assertThat(session.items().first().unitAmount.amount).isEqualTo("ISK123.00")
        assertThat(session.items().first().amountDetails.total.amount).isEqualTo("ISK246.00")
        assertConfirmed("isk", session)
    }

    @Test
    fun `French locale preserves actual decimal price and minor units`() {
        val session = parse("details-init").session(Locale.FRANCE)
        val ribbon = session.items()[1]
        assertThat(ribbon.unitAmountDecimal?.minorUnitsAmount).isEqualTo(BigDecimal("12.345678901234"))
        assertThat(ribbon.unitAmountDecimal?.amount).contains("0,12345678901234")
        assertThat(session.totals.total.amount).contains("51,32")
        assertThat(session.totals.total.minorUnitsAmount).isEqualTo(5132L)
    }

    private fun assertConfirmed(name: String, before: Session) {
        val response = parse("$name-confirm")
        val intent = requireNotNull(PaymentIntentJsonParser().parse(read("$name-payment-intent")))
        val session = response.withIntent(intent).session()
        assertThat(session.status).isEqualTo(Session.Status.Complete(Session.Status.PaymentStatus.Paid))
        assertThat(session.orderSummaryItems).isEqualTo(before.orderSummaryItems)
        assertThat(session.totals).isEqualTo(before.totals)
        assertThat(session.taxAmounts).isEqualTo(before.taxAmounts)
        assertThat(response.amount).isEqualTo(before.totals.total.minorUnitsAmount)
        assertThat(intent.amount).isEqualTo(before.totals.total.minorUnitsAmount)
    }

    private fun Session.items() = orderSummaryItems.flatMap { (it as Session.OrderSummaryItem.OneTimePrice).items }

    private fun CheckoutSessionResponse.session(locale: Locale = Locale.US) = asCheckoutSession(
        collectedEmail = null,
        collectedShippingName = null,
        collectedShippingAddress = null,
        flagImages = null,
        paymentOption = null,
        availableExpressButtonTypes = emptyList(),
        locale = locale,
    )

    private fun parse(name: String): CheckoutSessionResponse {
        val json = read(name)
        return requireNotNull(CheckoutSessionResponseJsonParser(
            CheckoutSessionClientSecret("${json.getString("id")}_secret_redacted")
        ).parse(json))
    }

    private fun read(name: String): JSONObject {
        val directory = generateSequence(File(".").absoluteFile) { it.parentFile }
            .map { File(it, "bug-bash/order-summary") }.first { it.isDirectory }
        return JSONObject(File(directory, "$name.json").readText())
    }
}
