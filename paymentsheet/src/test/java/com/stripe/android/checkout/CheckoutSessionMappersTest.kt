@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import org.junit.Test
import java.util.Locale

class CheckoutSessionMappersTest {
    @Test
    fun `maps unified public fields and totals`() {
        val response = CheckoutSessionResponseFactory.create(
            businessName = "Widgets, Inc.",
            liveMode = true,
            customerEmail = "server@example.com",
            checkoutItems = listOf(
                CheckoutSessionResponseFactory.checkoutItem(
                    total = 1080,
                    subtotal = 1000,
                    unitAmount = 500,
                    quantity = 2,
                    name = "Widget",
                ).withTaxes(inclusive = 20, exclusive = 80)
            ),
        )

        val session = response.session()

        assertThat(session.businessName).isEqualTo("Widgets, Inc.")
        assertThat(session.livemode).isTrue()
        assertThat(session.email).isEqualTo("server@example.com")
        assertThat(session.minorUnitsAmountDivisor).isEqualTo(100)
        assertThat(session.totals.subtotal.minorUnitsAmount).isEqualTo(1000.0)
        assertThat(session.totals.taxInclusive.minorUnitsAmount).isEqualTo(20.0)
        assertThat(session.totals.taxExclusive.minorUnitsAmount).isEqualTo(80.0)
        assertThat(session.totals.discount.minorUnitsAmount).isEqualTo(0.0)
        assertThat(session.totals.total.minorUnitsAmount).isEqualTo(1080.0)
    }

    @Test
    fun `maps nested item using stable inner key and server computed amounts`() {
        val item = CheckoutSessionResponseFactory.create(
            checkoutItems = listOf(CheckoutSessionResponseFactory.checkoutItem(total = 2500, subtotal = 2400))
        ).session().orderSummaryItems.single() as CheckoutController.Session.OrderSummaryItem.OneTimePrice

        assertThat(item.key).isEqualTo("group_1")
        assertThat(item.items.single().key).isEqualTo("item_1")
        assertThat(item.items.single().amountDetails.subtotal.minorUnitsAmount).isEqualTo(2400.0)
        assertThat(item.items.single().amountDetails.total.minorUnitsAmount).isEqualTo(2500.0)
    }

    @Test
    fun `formats USD JPY and sub-cent values with injected locale`() {
        val usd = CheckoutSessionResponseFactory.create(amount = 1234).session()
        val jpy = CheckoutSessionResponseFactory.create(amount = 1234, currency = "jpy").session()
        val decimalItem = CheckoutSessionResponseFactory.checkoutItem().let { group ->
            group.copy(
                oneTimePrice = group.oneTimePrice.copy(
                    items = group.oneTimePrice.items.map {
                        it.copy(unitAmountDecimal = 12.345678901234)
                    }
                )
            )
        }
        val decimal = CheckoutSessionResponseFactory.create(checkoutItems = listOf(decimalItem)).session()

        assertThat(usd.totals.total.amount).isEqualTo("$12.34")
        assertThat(jpy.totals.total.amount).isEqualTo("¥1,234")
        assertThat(
            (decimal.orderSummaryItems.single() as CheckoutController.Session.OrderSummaryItem.OneTimePrice)
                .items.single().unitAmountDecimal?.amount
        ).contains("0.12345678901234")
    }

    @Test
    fun `maps adaptive pricing currencies`() {
        val session = CheckoutSessionResponseFactory.create(
            currency = "eur",
            checkoutItems = listOf(CheckoutSessionResponseFactory.checkoutItem(currency = "eur")),
            adaptivePricingInfo = CheckoutSessionResponse.AdaptivePricingInfo(
                activePresentmentCurrency = "eur",
                integrationAmount = 1100,
                integrationCurrency = "usd",
                localCurrencyOptions = listOf(
                    CheckoutSessionResponse.LocalCurrencyOption(1000, 400, "eur", "0.90")
                ),
            ),
        ).session()

        assertThat(session.currency).isEqualTo("usd")
        assertThat(session.presentmentDetails?.presentmentCurrency).isEqualTo("eur")
    }

    @Test
    fun `complete status contains payment status`() {
        val status = CheckoutSessionResponseFactory.create(
            status = CheckoutSessionResponse.Status.COMPLETE,
            paymentStatus = CheckoutSessionResponse.PaymentStatus.PAID,
        ).session().status as CheckoutController.Session.Status.Complete

        assertThat(status.paymentStatus).isEqualTo(CheckoutController.Session.Status.PaymentStatus.Paid)
    }

    @Test
    fun `tax is nullable and derives pending state`() {
        val absent = CheckoutSessionResponseFactory.create(taxMeta = null).session()
        val pending = CheckoutSessionResponseFactory.create(
            taxMeta = CheckoutSessionResponse.TaxMeta(
                CheckoutSessionResponse.TaxComputationType.AUTOMATIC,
                CheckoutSessionResponse.TaxStatus.REQUIRES_LOCATION_INPUTS,
            ),
            taxAddressSource = CheckoutSessionResponse.TaxAddressSource.SHIPPING,
        ).session()

        assertThat(absent.tax).isNull()
        assertThat(pending.tax?.status).isEqualTo(CheckoutController.Session.Tax.Status.RequiresShippingAddress)
    }

    @Test
    fun `local email and shipping address override response state`() {
        val address = CheckoutController.Address.State("Denver", "US", "1 Main", null, "80202", "CO")
        val session = CheckoutSessionResponseFactory.create(customerEmail = "server@example.com").asCheckoutSession(
            collectedEmail = "local@example.com",
            collectedShippingName = "Jenny",
            collectedShippingAddress = address,
            flagImages = null,
            paymentOption = null,
            availableExpressButtonTypes = emptyList(),
            locale = Locale.US,
        )

        assertThat(session.email).isEqualTo("local@example.com")
        assertThat(session.shippingAddress?.name).isEqualTo("Jenny")
        assertThat(session.shippingAddress?.address?.postalCode).isEqualTo("80202")
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

    private fun CheckoutSessionResponse.CheckoutItem.withTaxes(
        inclusive: Long,
        exclusive: Long,
    ): CheckoutSessionResponse.CheckoutItem = copy(
        oneTimePrice = oneTimePrice.copy(
            items = oneTimePrice.items.map {
                it.copy(taxInclusive = inclusive, taxExclusive = exclusive)
            }
        )
    )
}
