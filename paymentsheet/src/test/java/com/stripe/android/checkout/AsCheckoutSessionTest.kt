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
        val session = createResponse(id = "cs_test_123").asCheckoutSession(
            billingAddress = null,
            shippingAddress = null,
        )
        assertThat(session.id).isEqualTo("cs_test_123")
    }

    @Test
    fun `maps currency`() {
        val session = createResponse(currency = "eur").asCheckoutSession(
            billingAddress = null,
            shippingAddress = null,
        )
        assertThat(session.currency).isEqualTo("eur")
    }

    @Test
    fun `null totalSummary maps to null`() {
        val session = createResponse(totalSummary = null).asCheckoutSession(
            billingAddress = null,
            shippingAddress = null,
        )
        assertThat(session.totalSummary).isNull()
    }

    @Test
    fun `maps totalSummary subtotal`() {
        val session = createResponse(
            totalSummary = TotalSummaryResponseFactory.create(subtotal = 5000L),
        ).asCheckoutSession(
            billingAddress = null,
            shippingAddress = null,
        )
        assertThat(session.totalSummary?.subtotal).isEqualTo(5000L)
    }

    @Test
    fun `maps totalSummary totalDueToday`() {
        val session = createResponse(
            totalSummary = TotalSummaryResponseFactory.create(totalDueToday = 4044L),
        ).asCheckoutSession(
            billingAddress = null,
            shippingAddress = null,
        )
        assertThat(session.totalSummary?.totalDueToday).isEqualTo(4044L)
    }

    @Test
    fun `maps totalSummary totalAmountDue`() {
        val session = createResponse(
            totalSummary = TotalSummaryResponseFactory.create(totalAmountDue = 3000L),
        ).asCheckoutSession(
            billingAddress = null,
            shippingAddress = null,
        )
        assertThat(session.totalSummary?.totalAmountDue).isEqualTo(3000L)
    }

    @Test
    fun `maps discountAmounts`() {
        val session = createResponse(
            totalSummary = TotalSummaryResponseFactory.create(
                discountAmounts = listOf(
                    CheckoutSessionResponse.DiscountAmount(amount = 500L, displayName = "SUMMER10"),
                    CheckoutSessionResponse.DiscountAmount(amount = 250L, displayName = "LOYALTY5"),
                ),
            ),
        ).asCheckoutSession(
            billingAddress = null,
            shippingAddress = null,
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
        val session = createResponse(
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
        ).asCheckoutSession(
            billingAddress = null,
            shippingAddress = null,
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
        val session = createResponse(
            totalSummary = TotalSummaryResponseFactory.create(
                shippingRate = CheckoutSessionResponse.ShippingRate(
                    id = "shr_standard",
                    amount = 500L,
                    displayName = "Standard Shipping",
                    deliveryEstimate = "5-7 business days",
                ),
            ),
        ).asCheckoutSession(
            billingAddress = null,
            shippingAddress = null,
        )
        val shipping = session.totalSummary!!.shippingRate!!
        assertThat(shipping.id).isEqualTo("shr_standard")
        assertThat(shipping.amount).isEqualTo(500L)
        assertThat(shipping.displayName).isEqualTo("Standard Shipping")
        assertThat(shipping.deliveryEstimate).isEqualTo("5-7 business days")
    }

    @Test
    fun `null shippingRate maps to null`() {
        val session = createResponse(
            totalSummary = TotalSummaryResponseFactory.create(shippingRate = null),
        ).asCheckoutSession(
            billingAddress = null,
            shippingAddress = null,
        )
        assertThat(session.totalSummary!!.shippingRate).isNull()
    }

    @Test
    fun `maps appliedBalance`() {
        val session = createResponse(
            totalSummary = TotalSummaryResponseFactory.create(appliedBalance = -200L),
        ).asCheckoutSession(
            billingAddress = null,
            shippingAddress = null,
        )
        assertThat(session.totalSummary!!.appliedBalance).isEqualTo(-200L)
    }

    @Test
    fun `null appliedBalance maps to null`() {
        val session = createResponse(
            totalSummary = TotalSummaryResponseFactory.create(appliedBalance = null),
        ).asCheckoutSession(
            billingAddress = null,
            shippingAddress = null,
        )
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
        ).asCheckoutSession(
            billingAddress = null,
            shippingAddress = null,
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
        val session = createResponse().asCheckoutSession(
            billingAddress = null,
            shippingAddress = null,
        )
        assertThat(session.lineItems).isEmpty()
    }

    @Test
    fun `maps shippingOptions`() {
        val session = createResponse(
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
        ).asCheckoutSession(
            billingAddress = null,
            shippingAddress = null,
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
        val session = createResponse().asCheckoutSession(
            billingAddress = null,
            shippingAddress = null,
        )
        assertThat(session.shippingOptions).isEmpty()
    }

    @Test
    fun `maps status open`() {
        val session = createResponse(status = "open").asCheckoutSession(
            billingAddress = null,
            shippingAddress = null,
        )
        assertThat(session.status).isEqualTo(CheckoutSession.Status.OPEN)
    }

    @Test
    fun `maps status complete`() {
        val session = createResponse(status = "complete").asCheckoutSession(
            billingAddress = null,
            shippingAddress = null,
        )
        assertThat(session.status).isEqualTo(CheckoutSession.Status.COMPLETE)
    }

    @Test
    fun `maps status expired`() {
        val session = createResponse(status = "expired").asCheckoutSession(
            billingAddress = null,
            shippingAddress = null,
        )
        assertThat(session.status).isEqualTo(CheckoutSession.Status.EXPIRED)
    }

    @Test
    fun `maps unknown status to UNKNOWN`() {
        val session = createResponse(status = "something_new").asCheckoutSession(
            billingAddress = null,
            shippingAddress = null,
        )
        assertThat(session.status).isEqualTo(CheckoutSession.Status.UNKNOWN)
    }

    @Test
    fun `maps paymentStatus paid`() {
        val session = createResponse(paymentStatus = "paid").asCheckoutSession(
            billingAddress = null,
            shippingAddress = null,
        )
        assertThat(session.paymentStatus).isEqualTo(CheckoutSession.PaymentStatus.PAID)
    }

    @Test
    fun `maps paymentStatus unpaid`() {
        val session = createResponse(paymentStatus = "unpaid").asCheckoutSession(
            billingAddress = null,
            shippingAddress = null,
        )
        assertThat(session.paymentStatus).isEqualTo(CheckoutSession.PaymentStatus.UNPAID)
    }

    @Test
    fun `maps paymentStatus no_payment_required`() {
        val session = createResponse(paymentStatus = "no_payment_required").asCheckoutSession(
            billingAddress = null,
            shippingAddress = null,
        )
        assertThat(session.paymentStatus).isEqualTo(CheckoutSession.PaymentStatus.NO_PAYMENT_REQUIRED)
    }

    @Test
    fun `maps unknown paymentStatus to UNKNOWN`() {
        val session = createResponse(paymentStatus = "something_new").asCheckoutSession(
            billingAddress = null,
            shippingAddress = null,
        )
        assertThat(session.paymentStatus).isEqualTo(CheckoutSession.PaymentStatus.UNKNOWN)
    }

    @Test
    fun `maps livemode`() {
        val session = createResponse(livemode = true).asCheckoutSession(
            billingAddress = null,
            shippingAddress = null,
        )
        assertThat(session.liveMode).isTrue()
    }

    @Test
    fun `maps customer id`() {
        val session = createResponse(
            customer = CheckoutSessionResponse.Customer(
                id = "cus_123",
                paymentMethods = emptyList(),
                canDetachPaymentMethod = false,
            ),
        ).asCheckoutSession(
            billingAddress = null,
            shippingAddress = null,
        )
        assertThat(session.customer).isEqualTo("cus_123")
    }

    @Test
    fun `null customer maps to null`() {
        val session = createResponse(customer = null).asCheckoutSession(
            billingAddress = null,
            shippingAddress = null,
        )
        assertThat(session.customer).isNull()
    }

    @Test
    fun `maps customerEmail`() {
        val session = createResponse(customerEmail = "test@example.com").asCheckoutSession(
            billingAddress = null,
            shippingAddress = null,
        )
        assertThat(session.customerEmail).isEqualTo("test@example.com")
    }

    @Test
    fun `null customerEmail maps to null`() {
        val session = createResponse(customerEmail = null).asCheckoutSession(
            billingAddress = null,
            shippingAddress = null,
        )
        assertThat(session.customerEmail).isNull()
    }

    @Test
    fun `maps top-level discountAmounts from totalSummary`() {
        val session = createResponse(
            totalSummary = TotalSummaryResponseFactory.create(
                discountAmounts = listOf(
                    CheckoutSessionResponse.DiscountAmount(amount = 500L, displayName = "SUMMER10"),
                ),
            ),
        ).asCheckoutSession(
            billingAddress = null,
            shippingAddress = null,
        )
        assertThat(session.discountAmounts).hasSize(1)
        assertThat(session.discountAmounts[0].amount).isEqualTo(500L)
        assertThat(session.discountAmounts[0].displayName).isEqualTo("SUMMER10")
    }

    @Test
    fun `top-level discountAmounts empty when no totalSummary`() {
        val session = createResponse(totalSummary = null).asCheckoutSession(
            billingAddress = null,
            shippingAddress = null,
        )
        assertThat(session.discountAmounts).isEmpty()
    }

    @Test
    fun `maps discountTotal in totalSummary`() {
        val session = createResponse(
            totalSummary = TotalSummaryResponseFactory.create(discountTotal = 750L),
        ).asCheckoutSession(
            billingAddress = null,
            shippingAddress = null,
        )
        assertThat(session.totalSummary!!.discountTotal).isEqualTo(750L)
    }

    @Test
    fun `null discountTotal maps to null`() {
        val session = createResponse(
            totalSummary = TotalSummaryResponseFactory.create(discountTotal = null),
        ).asCheckoutSession(
            billingAddress = null,
            shippingAddress = null,
        )
        assertThat(session.totalSummary!!.discountTotal).isNull()
    }

    @Test
    fun `maps billingAddress`() {
        val billingAddress = Address.State(
            city = "San Francisco",
            country = "US",
            line1 = "123 Market St",
            line2 = "Suite 100",
            postalCode = "94105",
            state = "CA",
        )
        val session = createResponse().asCheckoutSession(
            billingAddress = billingAddress,
            shippingAddress = null,
        )
        assertThat(session.billingAddress).isNotNull()
        assertThat(session.billingAddress!!.city).isEqualTo("San Francisco")
        assertThat(session.billingAddress.country).isEqualTo("US")
        assertThat(session.billingAddress.line1).isEqualTo("123 Market St")
        assertThat(session.billingAddress.line2).isEqualTo("Suite 100")
        assertThat(session.billingAddress.postalCode).isEqualTo("94105")
        assertThat(session.billingAddress.state).isEqualTo("CA")
    }

    @Test
    fun `null billingAddress maps to null`() {
        val session = createResponse().asCheckoutSession(
            billingAddress = null,
            shippingAddress = null,
        )
        assertThat(session.billingAddress).isNull()
    }

    @Test
    fun `maps shippingAddress`() {
        val shippingAddress = Address.State(
            city = "New York",
            country = "US",
            line1 = "456 Broadway",
            line2 = null,
            postalCode = "10013",
            state = "NY",
        )
        val session = createResponse().asCheckoutSession(
            billingAddress = null,
            shippingAddress = shippingAddress,
        )
        assertThat(session.shippingAddress).isNotNull()
        assertThat(session.shippingAddress!!.city).isEqualTo("New York")
        assertThat(session.shippingAddress.country).isEqualTo("US")
        assertThat(session.shippingAddress.line1).isEqualTo("456 Broadway")
        assertThat(session.shippingAddress.line2).isNull()
        assertThat(session.shippingAddress.postalCode).isEqualTo("10013")
        assertThat(session.shippingAddress.state).isEqualTo("NY")
    }

    @Test
    fun `null shippingAddress maps to null`() {
        val session = createResponse().asCheckoutSession(
            billingAddress = null,
            shippingAddress = null,
        )
        assertThat(session.shippingAddress).isNull()
    }

    private fun createResponse(
        id: String = DEFAULT_CHECKOUT_SESSION_ID,
        currency: String = "usd",
        status: String = "open",
        paymentStatus: String = "unpaid",
        livemode: Boolean = false,
        customerEmail: String? = null,
        customer: CheckoutSessionResponse.Customer? = null,
        totalSummary: CheckoutSessionResponse.TotalSummaryResponse? = null,
        lineItems: List<CheckoutSessionResponse.LineItem> = emptyList(),
        shippingOptions: List<CheckoutSessionResponse.ShippingRate> = emptyList(),
    ): CheckoutSessionResponse {
        return CheckoutSessionResponseFactory.create(
            id = id,
            currency = currency,
            status = status,
            paymentStatus = paymentStatus,
            livemode = livemode,
            customerEmail = customerEmail,
            customer = customer,
            totalSummary = totalSummary,
            lineItems = lineItems,
            shippingOptions = shippingOptions,
        )
    }
}
