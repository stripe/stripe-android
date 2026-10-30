@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import org.junit.Test
import java.util.Locale

class CheckoutSessionMappersTest {
    @Test
    fun `maps integration currency independently of adaptive pricing`() {
        val session = CheckoutSessionResponseFactory.create(
            currency = "eur",
            integrationCurrency = "usd",
        ).session()

        assertThat(session.currency).isEqualTo("usd")
        assertThat(session.totals?.total?.amount).isEqualTo("€10.00")
    }

    @Test
    fun `maps absent integration currency without affecting presentment amounts`() {
        val session = CheckoutSessionResponseFactory.create(
            integrationCurrency = null,
        ).session()

        assertThat(session.currency).isNull()
        assertThat(session.minorUnitsAmountDivisor).isEqualTo(100)
        assertThat(session.totals?.total?.amount).isEqualTo("$10.00")
    }

    @Test
    fun `divisor follows presentment currency when integration currency has different precision`() {
        val session = CheckoutSessionResponseFactory.create(
            currency = "usd",
            integrationCurrency = "jpy",
        ).session()

        assertThat(session.currency).isEqualTo("jpy")
        assertThat(session.minorUnitsAmountDivisor).isEqualTo(100)
        assertThat(session.totals?.total?.amount).isEqualTo("$10.00")
    }

    @Test
    fun `absent session currency does not use item currency for totals`() {
        val session = CheckoutSessionResponseFactory.create(
            currency = null,
            checkoutItems = listOf(CheckoutSessionResponseFactory.checkoutItem(currency = "jpy")),
            totalSummary = CheckoutSessionResponse.TotalSummary(
                totalTaxAmounts = listOf(
                    CheckoutSessionResponse.TaxAmount(
                        amount = 100,
                        inclusive = false,
                        taxRate = CheckoutSessionResponse.TaxRate("Tax", 10.0, null),
                    )
                ),
            ),
        ).session()

        assertThat(session.currency).isNull()
        assertThat(session.minorUnitsAmountDivisor).isNull()
        assertThat(session.totals).isNull()
        assertThat(session.taxAmounts).isNull()
    }

    @Test
    fun `public session falls back to the customer email`() {
        val session = CheckoutSessionResponseFactory.create(
            customer = CheckoutSessionResponse.Customer("cus_test", "customer@example.com", emptyList(), false),
        ).session()

        assertThat(session.email).isEqualTo("customer@example.com")
    }

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
        assertThat(session.totals?.subtotal?.minorUnitsAmount).isEqualTo(1000.0)
        assertThat(session.totals?.taxInclusive?.minorUnitsAmount).isEqualTo(20.0)
        assertThat(session.totals?.taxExclusive?.minorUnitsAmount).isEqualTo(80.0)
        assertThat(session.totals?.discount?.minorUnitsAmount).isEqualTo(0.0)
        assertThat(session.totals?.total?.minorUnitsAmount).isEqualTo(1080.0)
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

        assertThat(usd.totals?.total?.amount).isEqualTo("$12.34")
        assertThat(jpy.totals?.total?.amount).isEqualTo("¥1,234")
        assertThat(
            (decimal.orderSummaryItems.single() as CheckoutController.Session.OrderSummaryItem.OneTimePrice)
                .items.single().unitAmountDecimal?.amount
        ).contains("0.12345678901234")
    }

    @Test
    fun `maps adaptive pricing currencies`() {
        val session = CheckoutSessionResponseFactory.create(
            currency = "eur",
            integrationCurrency = "usd",
            checkoutItems = listOf(CheckoutSessionResponseFactory.checkoutItem(currency = "eur")),
            adaptivePricingInfo = CheckoutSessionResponse.AdaptivePricingInfo(
                activePresentmentCurrency = "eur",
                integrationAmount = 1100,
                integrationCurrency = "usd",
                integrationDisclosureCaption = null,
                localCurrencyOptions = listOf(
                    CheckoutSessionResponse.LocalCurrencyOption(1000, "eur", "1 USD = 0.90 EUR", null)
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
    fun `server email takes precedence while shipping comes from local state`() {
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

        assertThat(session.email).isEqualTo("server@example.com")
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
