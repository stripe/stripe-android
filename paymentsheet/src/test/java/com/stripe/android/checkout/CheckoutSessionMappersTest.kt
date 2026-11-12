@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CheckoutElementsController.Session
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import org.junit.Test
import java.math.BigDecimal
import java.util.Locale

class CheckoutSessionMappersTest {
    @Test
    fun `maps integration currency independently of adaptive pricing`() {
        val session = CheckoutSessionResponseFactory.create(
            currency = "eur",
            integrationCurrency = "usd",
        ).session()

        assertThat(session.currency).isEqualTo("usd")
        assertThat(session.totals.total.amount).isEqualTo("€10.00")
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
        assertThat(session.totals.subtotal.minorUnitsAmount).isEqualTo(1000L)
        assertThat(session.totals.taxInclusive.minorUnitsAmount).isEqualTo(20L)
        assertThat(session.totals.taxExclusive.minorUnitsAmount).isEqualTo(80L)
        assertThat(session.totals.discount.minorUnitsAmount).isEqualTo(0L)
        assertThat(session.totals.total.minorUnitsAmount).isEqualTo(1080L)
    }

    @Test
    fun `session totals use the server summary instead of item amounts`() {
        val taxRate = CheckoutSessionResponse.TaxRate(
            displayName = "Sales tax",
            percentage = 8.0,
            rateType = CheckoutSessionResponse.TaxRateType.PERCENTAGE,
        )
        val session = CheckoutSessionResponseFactory.create(
            checkoutItems = listOf(
                CheckoutSessionResponseFactory.checkoutItem(subtotal = 1000, total = 1080)
                    .withTaxes(inclusive = 20, exclusive = 80)
            ),
            totalSummary = CheckoutSessionResponse.TotalSummary(
                subtotal = 1500,
                total = 1625,
                taxExclusive = 125,
                taxInclusive = 40,
                totalTaxAmounts = listOf(
                    CheckoutSessionResponse.TaxAmount(amount = 125, inclusive = false, taxRate = taxRate),
                    CheckoutSessionResponse.TaxAmount(amount = 40, inclusive = true, taxRate = taxRate),
                ),
            ),
        ).session()
        val item = (session.orderSummaryItems.single() as Session.OrderSummaryItem.OneTimePrice).items.single()

        assertThat(session.totals).isEqualTo(
            Session.Totals(
                subtotal = Session.Amount("$15.00", 1500L),
                taxExclusive = Session.Amount("$1.25", 125L),
                taxInclusive = Session.Amount("$0.40", 40L),
                discount = Session.Amount("$0.00", 0L),
                total = Session.Amount("$16.25", 1625L),
            )
        )
        assertThat(item.amountDetails.subtotal.minorUnitsAmount).isEqualTo(1000L)
        assertThat(item.amountDetails.total.minorUnitsAmount).isEqualTo(1080L)
        assertThat(item.amountDetails.taxExclusive.minorUnitsAmount).isEqualTo(80L)
        assertThat(item.amountDetails.taxInclusive.minorUnitsAmount).isEqualTo(20L)
    }

    @Test
    fun `maps nested item using stable inner key and server computed amounts`() {
        val item = CheckoutSessionResponseFactory.create(
            checkoutItems = listOf(CheckoutSessionResponseFactory.checkoutItem(total = 2500, subtotal = 2400))
        ).session().orderSummaryItems.single() as Session.OrderSummaryItem.OneTimePrice

        assertThat(item.key).isEqualTo("group_1")
        assertThat(item.items.single().key).isEqualTo("item_1")
        assertThat(item.items.single().amountDetails.subtotal.minorUnitsAmount).isEqualTo(2400L)
        assertThat(item.items.single().amountDetails.total.minorUnitsAmount).isEqualTo(2500L)
    }

    @Test
    fun `formats USD JPY and sub-cent values with injected locale`() {
        val usd = CheckoutSessionResponseFactory.create(amount = 1234).session()
        val jpy = CheckoutSessionResponseFactory.create(amount = 1234, currency = "jpy").session()
        val decimalItem = CheckoutSessionResponseFactory.checkoutItem().let { group ->
            group.copy(
                oneTimePrice = group.oneTimePrice.copy(
                    items = group.oneTimePrice.items.map {
                        it.copy(unitAmountDecimal = BigDecimal("12.345678901234"))
                    }
                )
            )
        }
        val decimal = CheckoutSessionResponseFactory.create(checkoutItems = listOf(decimalItem)).session()

        assertThat(usd.totals.total.amount).isEqualTo("$12.34")
        assertThat(jpy.totals.total.amount).isEqualTo("¥1,234")
        assertThat(
            (decimal.orderSummaryItems.single() as Session.OrderSummaryItem.OneTimePrice)
                .items.single().unitAmountDecimal?.amount
        ).contains("0.12345678901234")
    }

    @Test
    fun `preserves integer amounts beyond double precision`() {
        val amount = 9007199254740993L
        val session = CheckoutSessionResponseFactory.create(amount = amount).session()
        val item = (session.orderSummaryItems.single() as Session.OrderSummaryItem.OneTimePrice)
            .items.single()

        assertThat(session.totals.total.minorUnitsAmount).isEqualTo(amount)
        assertThat(session.totals.total.amount).isEqualTo("$90,071,992,547,409.93")
        assertThat(item.unitAmount.minorUnitsAmount).isEqualTo(amount)
        assertThat(item.amountDetails.subtotal.minorUnitsAmount).isEqualTo(amount)
        assertThat(item.unitAmountDecimal).isNull()
    }

    @Test
    fun `preserves fractional minor units beyond double precision`() {
        val amount = BigDecimal("9007199254740993.123456789012")
        val group = CheckoutSessionResponseFactory.checkoutItem()
        val decimalItem = group.copy(
            oneTimePrice = group.oneTimePrice.copy(
                items = group.oneTimePrice.items.map { it.copy(unitAmountDecimal = amount) }
            )
        )
        val session = CheckoutSessionResponseFactory.create(checkoutItems = listOf(decimalItem)).session()
        val item = (session.orderSummaryItems.single() as Session.OrderSummaryItem.OneTimePrice)
            .items.single()

        assertThat(item.unitAmountDecimal?.minorUnitsAmount).isEqualTo(amount)
        assertThat(item.unitAmountDecimal?.amount).isEqualTo("$90,071,992,547,409.93123456789012")
    }

    @Test
    fun `maps integer tax amounts for items and session`() {
        val tax = CheckoutSessionResponse.TaxAmount(
            amount = 9007199254740993L,
            inclusive = false,
            taxRate = CheckoutSessionResponse.TaxRate(
                displayName = "Sales tax",
                percentage = 8.25,
                rateType = CheckoutSessionResponse.TaxRateType.PERCENTAGE,
            ),
        )
        val group = CheckoutSessionResponseFactory.checkoutItem()
        val taxedItem = group.copy(
            oneTimePrice = group.oneTimePrice.copy(
                items = group.oneTimePrice.items.map { it.copy(taxAmounts = listOf(tax)) }
            )
        )
        val session = CheckoutSessionResponseFactory.create(
            checkoutItems = listOf(taxedItem),
            totalSummary = CheckoutSessionResponse.TotalSummary(
                subtotal = 1000,
                total = 1000 + tax.amount,
                taxExclusive = tax.amount,
                taxInclusive = 0,
                totalTaxAmounts = listOf(tax),
            ),
        ).session()
        val item = (session.orderSummaryItems.single() as Session.OrderSummaryItem.OneTimePrice)
            .items.single()

        assertThat(session.taxAmounts?.single()?.minorUnitsAmount).isEqualTo(tax.amount)
        assertThat(session.taxAmounts?.single()?.amount).isEqualTo("$90,071,992,547,409.93")
        assertThat(item.amountDetails.taxAmounts?.single()?.minorUnitsAmount).isEqualTo(tax.amount)
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
        ).session().status as Session.Status.Complete

        assertThat(status.paymentStatus).isEqualTo(Session.Status.PaymentStatus.Paid)
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
        assertThat(pending.tax?.status).isEqualTo(Session.Tax.Status.RequiresShippingAddress)
    }

    @Test
    fun `server email takes precedence while shipping comes from local state`() {
        val address = CheckoutElementsController.Address.State("Denver", "US", "1 Main", null, "80202", "CO")
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
