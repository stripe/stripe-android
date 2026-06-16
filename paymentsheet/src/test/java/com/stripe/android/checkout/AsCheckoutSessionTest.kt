package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.repositories.TotalSummaryResponseFactory
import org.junit.Test

@OptIn(CheckoutSessionPreview::class)
class AsCheckoutSessionTest {

    @Test
    fun `maps id`() {
        val session = createSession(id = "cs_test_123")
        assertThat(session.id).isEqualTo("cs_test_123")
    }

    @Test
    fun `maps status open`() {
        val session = createSession(status = CheckoutSessionResponse.Status.OPEN)
        assertThat(session.status).isEqualTo(CheckoutSession.Status.Open)
    }

    @Test
    fun `maps status complete`() {
        val session = createSession(status = CheckoutSessionResponse.Status.COMPLETE)
        assertThat(session.status).isEqualTo(CheckoutSession.Status.Complete)
    }

    @Test
    fun `maps status expired`() {
        val session = createSession(status = CheckoutSessionResponse.Status.EXPIRED)
        assertThat(session.status).isEqualTo(CheckoutSession.Status.Expired)
    }

    @Test
    fun `maps status unknown`() {
        val session = createSession(status = CheckoutSessionResponse.Status.UNKNOWN)
        assertThat(session.status).isEqualTo(CheckoutSession.Status.Unknown)
    }

    @Test
    fun `maps livemode true`() {
        val session = createSession(liveMode = true)
        assertThat(session.liveMode).isTrue()
    }

    @Test
    fun `maps livemode false`() {
        val session = createSession(liveMode = false)
        assertThat(session.liveMode).isFalse()
    }

    @Test
    fun `maps currency`() {
        val session = createSession(currency = "eur")
        assertThat(session.currency).isEqualTo("eur")
    }

    @Test
    fun `maps customerEmail`() {
        val session = createSession(customerEmail = "test@example.com")
        assertThat(session.customerEmail).isEqualTo("test@example.com")
    }

    @Test
    fun `null customerEmail maps to null`() {
        val session = createSession(customerEmail = null)
        assertThat(session.customerEmail).isNull()
    }

    @Test
    fun `maps tax status ready`() {
        val session = createSession(taxStatus = CheckoutSessionResponse.TaxStatus.READY)
        assertThat(session.tax.status).isEqualTo(CheckoutSession.Tax.Status.Ready)
    }

    @Test
    fun `maps tax status requires shipping address`() {
        val session = createSession(
            taxStatus = CheckoutSessionResponse.TaxStatus.REQUIRES_SHIPPING_ADDRESS
        )
        assertThat(session.tax.status).isEqualTo(CheckoutSession.Tax.Status.RequiresShippingAddress)
    }

    @Test
    fun `maps tax status requires billing address`() {
        val session = createSession(
            taxStatus = CheckoutSessionResponse.TaxStatus.REQUIRES_BILLING_ADDRESS
        )
        assertThat(session.tax.status).isEqualTo(CheckoutSession.Tax.Status.RequiresBillingAddress)
    }

    @Test
    fun `maps tax status unknown`() {
        val session = createSession(taxStatus = CheckoutSessionResponse.TaxStatus.UNKNOWN)
        assertThat(session.tax.status).isEqualTo(CheckoutSession.Tax.Status.Unknown)
    }

    @Test
    fun `null totalSummary maps to null`() {
        val session = createSession(totalSummary = null)
        assertThat(session.totalSummary).isNull()
    }

    @Test
    fun `maps totalSummary subtotal`() {
        val session = createSession(
            totalSummary = TotalSummaryResponseFactory.create(subtotal = 5000L),
        )
        assertThat(session.totalSummary?.subtotal).isEqualTo(5000L)
    }

    @Test
    fun `maps totalSummary totalDueToday`() {
        val session = createSession(
            totalSummary = TotalSummaryResponseFactory.create(totalDueToday = 4044L),
        )
        assertThat(session.totalSummary?.totalDueToday).isEqualTo(4044L)
    }

    @Test
    fun `maps totalSummary totalAmountDue`() {
        val session = createSession(
            totalSummary = TotalSummaryResponseFactory.create(totalAmountDue = 3000L),
        )
        assertThat(session.totalSummary?.totalAmountDue).isEqualTo(3000L)
    }

    @Test
    fun `maps discountAmounts`() {
        val session = createSession(
            totalSummary = TotalSummaryResponseFactory.create(
                discountAmounts = listOf(
                    CheckoutSessionResponse.DiscountAmount(amount = 500L, displayName = "SUMMER10"),
                    CheckoutSessionResponse.DiscountAmount(amount = 250L, displayName = "LOYALTY5"),
                ),
            ),
        )
        val discounts = session.totalSummary!!.discountAmounts
        assertThat(discounts).hasSize(2)
        assertThat(discounts[0].amount).isEqualTo(500L)
        assertThat(discounts[0].displayName).isEqualTo("SUMMER10")
        assertThat(discounts[1].amount).isEqualTo(250L)
        assertThat(discounts[1].displayName).isEqualTo("LOYALTY5")
    }

    @Test
    fun `maps taxAmounts`() {
        val session = createSession(
            totalSummary = TotalSummaryResponseFactory.create(
                taxAmounts = listOf(
                    CheckoutSessionResponse.TaxAmount(
                        amount = 294L,
                        inclusive = false,
                        displayName = "Sales Tax",
                        percentage = 6.875,
                    ),
                ),
            ),
        )
        val taxes = session.totalSummary!!.taxAmounts
        assertThat(taxes).hasSize(1)
        assertThat(taxes[0].amount).isEqualTo(294L)
        assertThat(taxes[0].inclusive).isFalse()
        assertThat(taxes[0].displayName).isEqualTo("Sales Tax")
        assertThat(taxes[0].percentage).isEqualTo(6.875)
    }

    @Test
    fun `maps shippingRate`() {
        val session = createSession(
            totalSummary = TotalSummaryResponseFactory.create(
                shippingRate = CheckoutSessionResponse.ShippingRate(
                    id = "shr_standard",
                    amount = 500L,
                    displayName = "Standard Shipping",
                    deliveryEstimate = "5-7 business days",
                ),
            ),
        )
        val shipping = session.totalSummary!!.shippingRate!!
        assertThat(shipping.id).isEqualTo("shr_standard")
        assertThat(shipping.amount).isEqualTo(500L)
        assertThat(shipping.displayName).isEqualTo("Standard Shipping")
        assertThat(shipping.deliveryEstimate).isEqualTo("5-7 business days")
    }

    @Test
    fun `null shippingRate maps to null`() {
        val session = createSession(
            totalSummary = TotalSummaryResponseFactory.create(shippingRate = null),
        )
        assertThat(session.totalSummary!!.shippingRate).isNull()
    }

    @Test
    fun `maps appliedBalance`() {
        val session = createSession(
            totalSummary = TotalSummaryResponseFactory.create(appliedBalance = -200L),
        )
        assertThat(session.totalSummary!!.appliedBalance).isEqualTo(-200L)
    }

    @Test
    fun `null appliedBalance maps to null`() {
        val session = createSession(
            totalSummary = TotalSummaryResponseFactory.create(appliedBalance = null),
        )
        assertThat(session.totalSummary!!.appliedBalance).isNull()
    }

    @Test
    fun `maps lineItems`() {
        val session = createSession(
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
        )
        val items = session.lineItems
        assertThat(items).hasSize(1)
        assertThat(items[0].id).isEqualTo("li_1")
        assertThat(items[0].name).isEqualTo("Llama Figure")
        assertThat(items[0].quantity).isEqualTo(2)
        assertThat(items[0].unitAmount).isEqualTo(999L)
        assertThat(items[0].subtotal).isEqualTo(1998L)
        assertThat(items[0].total).isEqualTo(1998L)
    }

    @Test
    fun `empty lineItems maps to empty list`() {
        val session = createSession()
        assertThat(session.lineItems).isEmpty()
    }

    @Test
    fun `maps shippingOptions`() {
        val session = createSession(
            shippingOptions = listOf(
                CheckoutSessionResponse.ShippingRate(
                    id = "shr_standard",
                    amount = 500L,
                    displayName = "Standard Shipping",
                    deliveryEstimate = null,
                ),
                CheckoutSessionResponse.ShippingRate(
                    id = "shr_express",
                    amount = 1500L,
                    displayName = "Express Shipping",
                    deliveryEstimate = "1-3 business days",
                ),
            ),
        )
        val options = session.shippingOptions
        assertThat(options).hasSize(2)
        assertThat(options[0].id).isEqualTo("shr_standard")
        assertThat(options[0].amount).isEqualTo(500L)
        assertThat(options[0].displayName).isEqualTo("Standard Shipping")
        assertThat(options[0].deliveryEstimate).isNull()
        assertThat(options[1].id).isEqualTo("shr_express")
        assertThat(options[1].amount).isEqualTo(1500L)
        assertThat(options[1].displayName).isEqualTo("Express Shipping")
        assertThat(options[1].deliveryEstimate).isEqualTo("1-3 business days")
    }

    @Test
    fun `empty shippingOptions maps to empty list`() {
        val session = createSession()
        assertThat(session.shippingOptions).isEmpty()
    }

    private fun createSession(
        id: String = DEFAULT_CHECKOUT_SESSION_ID,
        status: CheckoutSessionResponse.Status = CheckoutSessionResponse.Status.OPEN,
        liveMode: Boolean = false,
        currency: String = "usd",
        customerEmail: String? = null,
        taxStatus: CheckoutSessionResponse.TaxStatus = CheckoutSessionResponse.TaxStatus.READY,
        totalSummary: CheckoutSessionResponse.TotalSummaryResponse? = null,
        lineItems: List<CheckoutSessionResponse.LineItem> = emptyList(),
        shippingOptions: List<CheckoutSessionResponse.ShippingRate> = emptyList(),
    ): CheckoutSession {
        return InternalState(
            key = "CheckoutConfigurationMergerTest",
            configuration = Checkout.Configuration().build(),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(
                id = id,
                status = status,
                liveMode = liveMode,
                currency = currency,
                customerEmail = customerEmail,
                taxStatus = taxStatus,
                totalSummary = totalSummary,
                lineItems = lineItems,
                shippingOptions = shippingOptions,
            ),
            flagImages = null,
        ).asCheckoutSession()
    }
}
