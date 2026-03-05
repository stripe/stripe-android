package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse.TotalSummaryResponse
import org.junit.Test

@OptIn(CheckoutSessionPreview::class)
class AsCheckoutSessionTest {

    @Test
    fun `maps id`() {
        val session = createResponse(id = "cs_test_123").asCheckoutSession()
        assertThat(session.id).isEqualTo("cs_test_123")
    }

    @Test
    fun `maps currency`() {
        val session = createResponse(currency = "eur").asCheckoutSession()
        assertThat(session.currency).isEqualTo("eur")
    }

    @Test
    fun `null totalSummary maps to null`() {
        val session = createResponse(totalSummary = null).asCheckoutSession()
        assertThat(session.totalSummary).isNull()
    }

    @Test
    fun `maps totalSummary subtotal`() {
        val session = createResponse(
            totalSummary = createTotalSummary(subtotal = 5000L),
        ).asCheckoutSession()
        assertThat(session.totalSummary?.subtotal).isEqualTo(5000L)
    }

    @Test
    fun `maps totalSummary totalDueToday`() {
        val session = createResponse(
            totalSummary = createTotalSummary(totalDueToday = 4044L),
        ).asCheckoutSession()
        assertThat(session.totalSummary?.totalDueToday).isEqualTo(4044L)
    }

    @Test
    fun `maps totalSummary totalAmountDue`() {
        val session = createResponse(
            totalSummary = createTotalSummary(totalAmountDue = 3000L),
        ).asCheckoutSession()
        assertThat(session.totalSummary?.totalAmountDue).isEqualTo(3000L)
    }

    @Test
    fun `maps discountAmounts`() {
        val session = createResponse(
            totalSummary = createTotalSummary(
                discountAmounts = listOf(
                    CheckoutSessionResponse.DiscountAmount(amount = 500L, displayName = "SUMMER10"),
                    CheckoutSessionResponse.DiscountAmount(amount = 250L, displayName = "LOYALTY5"),
                ),
            ),
        ).asCheckoutSession()
        val discounts = session.totalSummary!!.discountAmounts
        assertThat(discounts).hasSize(2)
        assertThat(discounts[0].amount).isEqualTo(500L)
        assertThat(discounts[0].displayName).isEqualTo("SUMMER10")
        assertThat(discounts[1].amount).isEqualTo(250L)
        assertThat(discounts[1].displayName).isEqualTo("LOYALTY5")
    }

    @Test
    fun `maps taxAmounts`() {
        val session = createResponse(
            totalSummary = createTotalSummary(
                taxAmounts = listOf(
                    CheckoutSessionResponse.TaxAmount(
                        amount = 294L,
                        inclusive = false,
                        displayName = "Sales Tax",
                        percentage = 6.875,
                    ),
                ),
            ),
        ).asCheckoutSession()
        val taxes = session.totalSummary!!.taxAmounts
        assertThat(taxes).hasSize(1)
        assertThat(taxes[0].amount).isEqualTo(294L)
        assertThat(taxes[0].inclusive).isFalse()
        assertThat(taxes[0].displayName).isEqualTo("Sales Tax")
        assertThat(taxes[0].percentage).isEqualTo(6.875)
    }

    @Test
    fun `maps shippingRate`() {
        val session = createResponse(
            totalSummary = createTotalSummary(
                shippingRate = CheckoutSessionResponse.ShippingRate(
                    amount = 500L,
                    displayName = "Standard Shipping",
                    deliveryEstimate = "5-7 business days",
                ),
            ),
        ).asCheckoutSession()
        val shipping = session.totalSummary!!.shippingRate!!
        assertThat(shipping.amount).isEqualTo(500L)
        assertThat(shipping.displayName).isEqualTo("Standard Shipping")
        assertThat(shipping.deliveryEstimate).isEqualTo("5-7 business days")
    }

    @Test
    fun `null shippingRate maps to null`() {
        val session = createResponse(
            totalSummary = createTotalSummary(shippingRate = null),
        ).asCheckoutSession()
        assertThat(session.totalSummary!!.shippingRate).isNull()
    }

    @Test
    fun `maps appliedBalance`() {
        val session = createResponse(
            totalSummary = createTotalSummary(appliedBalance = -200L),
        ).asCheckoutSession()
        assertThat(session.totalSummary!!.appliedBalance).isEqualTo(-200L)
    }

    @Test
    fun `null appliedBalance maps to null`() {
        val session = createResponse(
            totalSummary = createTotalSummary(appliedBalance = null),
        ).asCheckoutSession()
        assertThat(session.totalSummary!!.appliedBalance).isNull()
    }

    @Test
    fun `maps lineItems`() {
        val session = createResponse(
            lineItems = listOf(
                CheckoutSessionResponse.LineItem(
                    id = "li_1",
                    name = "Llama Figure",
                    quantity = 2,
                    unitAmount = 999L,
                    subtotal = 1998L,
                    total = 1998L,
                ),
            ),
        ).asCheckoutSession()
        val items = session.lineItems
        assertThat(items).hasSize(1)
        assertThat(items[0].name).isEqualTo("Llama Figure")
        assertThat(items[0].quantity).isEqualTo(2)
        assertThat(items[0].unitAmount).isEqualTo(999L)
        assertThat(items[0].subtotal).isEqualTo(1998L)
        assertThat(items[0].total).isEqualTo(1998L)
    }

    @Test
    fun `empty lineItems maps to empty list`() {
        val session = createResponse().asCheckoutSession()
        assertThat(session.lineItems).isEmpty()
    }

    private fun createResponse(
        id: String = "cs_test_abc123",
        currency: String = "usd",
        totalSummary: TotalSummaryResponse? = null,
        lineItems: List<CheckoutSessionResponse.LineItem> = emptyList(),
    ): CheckoutSessionResponse {
        return CheckoutSessionResponse(
            id = id,
            amount = 1000L,
            currency = currency,
            totalSummary = totalSummary,
            lineItems = lineItems,
        )
    }

    private fun createTotalSummary(
        subtotal: Long = 1000L,
        totalDueToday: Long = 1000L,
        totalAmountDue: Long = 1000L,
        discountAmounts: List<CheckoutSessionResponse.DiscountAmount> = emptyList(),
        taxAmounts: List<CheckoutSessionResponse.TaxAmount> = emptyList(),
        shippingRate: CheckoutSessionResponse.ShippingRate? = null,
        appliedBalance: Long? = null,
    ): TotalSummaryResponse {
        return TotalSummaryResponse(
            subtotal = subtotal,
            totalDueToday = totalDueToday,
            totalAmountDue = totalAmountDue,
            discountAmounts = discountAmounts,
            taxAmounts = taxAmounts,
            shippingRate = shippingRate,
            appliedBalance = appliedBalance,
        )
    }
}
