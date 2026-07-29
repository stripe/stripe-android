package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CheckoutController.Session
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.repositories.TotalSummaryResponseFactory
import org.junit.Test

@OptIn(CheckoutSessionPreview::class)
class CheckoutSessionMappersTest {

    @Test
    fun `maps id`() {
        val session = createSession(id = "cs_test_123")
        assertThat(session.id).isEqualTo("cs_test_123")
    }

    @Test
    fun `maps status open`() {
        val session = createSession(status = CheckoutSessionResponse.Status.OPEN)
        assertThat(session.status).isEqualTo(Session.Status.Open)
    }

    @Test
    fun `maps status complete`() {
        val session = createSession(status = CheckoutSessionResponse.Status.COMPLETE)
        assertThat(session.status).isInstanceOf(Session.Status.Complete::class.java)
    }

    @Test
    fun `maps status expired`() {
        val session = createSession(status = CheckoutSessionResponse.Status.EXPIRED)
        assertThat(session.status).isEqualTo(Session.Status.Expired)
    }

    @Test
    fun `unknown status maps to open`() {
        val session = createSession(status = CheckoutSessionResponse.Status.UNKNOWN)
        assertThat(session.status).isEqualTo(Session.Status.Open)
    }

    @Test
    fun `complete setup-mode session has NoPaymentRequired payment status`() {
        val session = createSession(
            status = CheckoutSessionResponse.Status.COMPLETE,
            mode = CheckoutSessionResponse.Mode.SETUP,
        )
        val complete = session.status as Session.Status.Complete
        assertThat(complete.paymentStatus).isEqualTo(Session.Status.PaymentStatus.NoPaymentRequired)
    }

    @Test
    fun `complete zero-amount session has NoPaymentRequired payment status`() {
        val session = createSession(status = CheckoutSessionResponse.Status.COMPLETE, amount = 0L)
        val complete = session.status as Session.Status.Complete
        assertThat(complete.paymentStatus).isEqualTo(Session.Status.PaymentStatus.NoPaymentRequired)
    }

    @Test
    fun `complete session with a succeeded PaymentIntent has Paid payment status`() {
        val session = createSession(
            status = CheckoutSessionResponse.Status.COMPLETE,
            paymentIntent = PaymentIntentFixtures.PI_SUCCEEDED,
        )
        val complete = session.status as Session.Status.Complete
        assertThat(complete.paymentStatus).isEqualTo(Session.Status.PaymentStatus.Paid)
    }

    @Test
    fun `complete session without a collected payment has Unpaid payment status`() {
        val session = createSession(
            status = CheckoutSessionResponse.Status.COMPLETE,
            paymentIntent = null,
        )
        val complete = session.status as Session.Status.Complete
        assertThat(complete.paymentStatus).isEqualTo(Session.Status.PaymentStatus.Unpaid)
    }

    @Test
    fun `maps businessName`() {
        val session = createSession(businessName = "Example, Inc.")
        assertThat(session.businessName).isEqualTo("Example, Inc.")
    }

    @Test
    fun `null businessName maps to null`() {
        val session = createSession(businessName = null)
        assertThat(session.businessName).isNull()
    }

    @Test
    fun `maps lastPaymentError from the PaymentIntent`() {
        val session = createSession(paymentIntent = PaymentIntentFixtures.PI_WITH_LAST_PAYMENT_ERROR)
        assertThat(session.lastPaymentError?.message)
            .isEqualTo(PaymentIntentFixtures.PI_WITH_LAST_PAYMENT_ERROR?.lastPaymentError?.message)
    }

    @Test
    fun `no last payment error maps to null`() {
        val session = createSession(paymentIntent = PaymentIntentFixtures.PI_SUCCEEDED)
        assertThat(session.lastPaymentError).isNull()
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
    fun `maps email`() {
        val session = createSession(customerEmail = "test@example.com")
        assertThat(session.email).isEqualTo("test@example.com")
    }

    @Test
    fun `null email maps to null`() {
        val session = createSession(customerEmail = null)
        assertThat(session.email).isNull()
    }

    @Test
    fun `maps presentmentDetails from adaptive pricing info`() {
        val session = createSession(
            adaptivePricingInfo = CheckoutSessionResponse.AdaptivePricingInfo(
                activePresentmentCurrency = "eur",
                integrationAmount = 1000L,
                integrationCurrency = "usd",
                localCurrencyOptions = emptyList(),
            ),
        )
        assertThat(session.presentmentDetails?.presentmentCurrency).isEqualTo("eur")
    }

    @Test
    fun `null adaptive pricing info maps presentmentDetails to null`() {
        val session = createSession(adaptivePricingInfo = null)
        assertThat(session.presentmentDetails).isNull()
    }

    @Test
    fun `maps shippingAddress from collected details`() {
        val session = createSession(
            collectedDetails = CheckoutCollectedDetails(
                shippingName = "Jane Doe",
                shippingAddress = CheckoutController.Address.State(
                    city = "Denver",
                    country = "US",
                    line1 = "123 Main St",
                    line2 = "Apt 4",
                    postalCode = "80202",
                    state = "CO",
                ),
            ),
        )

        val shippingAddress = requireNotNull(session.shippingAddress)
        assertThat(shippingAddress.name).isEqualTo("Jane Doe")
        assertThat(shippingAddress.address.country).isEqualTo("US")
        assertThat(shippingAddress.address.line1).isEqualTo("123 Main St")
        assertThat(shippingAddress.address.line2).isEqualTo("Apt 4")
        assertThat(shippingAddress.address.city).isEqualTo("Denver")
        assertThat(shippingAddress.address.postalCode).isEqualTo("80202")
        assertThat(shippingAddress.address.state).isEqualTo("CO")
    }

    @Test
    fun `no collected shipping address maps shippingAddress to null`() {
        val session = createSession(collectedDetails = CheckoutCollectedDetails())
        assertThat(session.shippingAddress).isNull()
    }

    @Test
    fun `maps tax status ready`() {
        val session = createSession(taxStatus = CheckoutSessionResponse.TaxStatus.READY)
        assertThat(session.tax.status).isEqualTo(Session.Tax.Status.Ready)
    }

    @Test
    fun `maps tax status requires shipping address`() {
        val session = createSession(
            taxStatus = CheckoutSessionResponse.TaxStatus.REQUIRES_SHIPPING_ADDRESS
        )
        assertThat(session.tax.status).isEqualTo(Session.Tax.Status.RequiresShippingAddress)
    }

    @Test
    fun `maps tax status requires billing address`() {
        val session = createSession(
            taxStatus = CheckoutSessionResponse.TaxStatus.REQUIRES_BILLING_ADDRESS
        )
        assertThat(session.tax.status).isEqualTo(Session.Tax.Status.RequiresBillingAddress)
    }

    @Test
    fun `unknown tax status maps to requires shipping address`() {
        val session = createSession(taxStatus = CheckoutSessionResponse.TaxStatus.UNKNOWN)
        assertThat(session.tax.status).isEqualTo(Session.Tax.Status.RequiresShippingAddress)
    }

    @Test
    fun `null totalSummary maps to null`() {
        val session = createSession(totalSummary = null)
        assertThat(session.totalSummary).isNull()
    }

    @Test
    fun `null totalSummary maps totals to null`() {
        val session = createSession(totalSummary = null)
        assertThat(session.totals).isNull()
    }

    @Test
    fun `derives totals breakdown from the total summary`() {
        val session = createSession(
            totalSummary = TotalSummaryResponseFactory.create(
                subtotal = 5000L,
                totalAmountDue = 5400L,
                discountAmounts = listOf(
                    CheckoutSessionResponse.DiscountAmount(amount = 200L, displayName = "SUMMER10"),
                ),
                taxAmounts = listOf(
                    CheckoutSessionResponse.TaxAmount(
                        amount = 400L, inclusive = false, displayName = "Sales Tax", percentage = 8.0,
                    ),
                    CheckoutSessionResponse.TaxAmount(
                        amount = 100L, inclusive = true, displayName = "VAT", percentage = 2.0,
                    ),
                ),
            ),
        )

        val totals = requireNotNull(session.totals)
        assertThat(totals.subtotal.minorUnitsAmount).isEqualTo(5000L)
        assertThat(totals.taxExclusive.minorUnitsAmount).isEqualTo(400L)
        assertThat(totals.taxInclusive.minorUnitsAmount).isEqualTo(100L)
        assertThat(totals.discount.minorUnitsAmount).isEqualTo(200L)
        assertThat(totals.total.minorUnitsAmount).isEqualTo(5400L)
    }

    @Test
    fun `maps discount promotion code and percent off`() {
        val session = createSession(
            totalSummary = TotalSummaryResponseFactory.create(
                discountAmounts = listOf(
                    CheckoutSessionResponse.DiscountAmount(
                        amount = 500L,
                        displayName = "SUMMER10",
                        promotionCode = "SUMMER",
                        percentOff = 10,
                    ),
                ),
            ),
        )
        val discount = session.totalSummary!!.discountAmounts.single()
        assertThat(discount.promotionCode).isEqualTo("SUMMER")
        assertThat(discount.percentOff).isEqualTo(10)
    }

    @Test
    fun `maps totalSummary subtotal`() {
        val session = createSession(
            totalSummary = TotalSummaryResponseFactory.create(subtotal = 5000L),
        )
        assertThat(session.totalSummary?.subtotal?.minorUnitsAmount).isEqualTo(5000L)
    }

    @Test
    fun `formats totalSummary subtotal`() {
        val session = createSession(
            currency = "usd",
            totalSummary = TotalSummaryResponseFactory.create(subtotal = 5000L),
        )
        // The formatted string is locale-dependent, but the major-unit value is always present.
        assertThat(session.totalSummary?.subtotal?.formatted).contains("50")
    }

    @Test
    fun `maps totalSummary totalDueToday`() {
        val session = createSession(
            totalSummary = TotalSummaryResponseFactory.create(totalDueToday = 4044L),
        )
        assertThat(session.totalSummary?.totalDueToday?.minorUnitsAmount).isEqualTo(4044L)
    }

    @Test
    fun `maps totalSummary totalAmountDue`() {
        val session = createSession(
            totalSummary = TotalSummaryResponseFactory.create(totalAmountDue = 3000L),
        )
        assertThat(session.totalSummary?.totalAmountDue?.minorUnitsAmount).isEqualTo(3000L)
    }

    @Test
    fun `maps minorUnitsAmountDivisor for a two-decimal currency`() {
        val session = createSession(currency = "usd")
        assertThat(session.minorUnitsAmountDivisor).isEqualTo(100)
    }

    @Test
    fun `maps minorUnitsAmountDivisor for a zero-decimal currency`() {
        val session = createSession(currency = "jpy")
        assertThat(session.minorUnitsAmountDivisor).isEqualTo(1)
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
        assertThat(discounts[0].amount.minorUnitsAmount).isEqualTo(500L)
        assertThat(discounts[0].displayName).isEqualTo("SUMMER10")
        assertThat(discounts[1].amount.minorUnitsAmount).isEqualTo(250L)
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
        assertThat(taxes[0].amount.minorUnitsAmount).isEqualTo(294L)
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
        assertThat(shipping.amount.minorUnitsAmount).isEqualTo(500L)
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
        assertThat(session.totalSummary!!.appliedBalance?.minorUnitsAmount).isEqualTo(-200L)
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
        assertThat(items[0].unitAmount?.minorUnitsAmount).isEqualTo(999L)
        assertThat(items[0].subtotal.minorUnitsAmount).isEqualTo(1998L)
        assertThat(items[0].total.minorUnitsAmount).isEqualTo(1998L)
    }

    @Test
    fun `empty lineItems maps to empty list`() {
        val session = createSession()
        assertThat(session.lineItems).isEmpty()
    }

    @Test
    fun `maps lineItems into a single orderSummaryItems group`() {
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
            totalSummary = TotalSummaryResponseFactory.create(subtotal = 1998L, totalAmountDue = 2100L),
        )

        val group = session.orderSummaryItems.single() as Session.OrderSummaryItem.OneTimePrice
        assertThat(group.subtotal?.minorUnitsAmount).isEqualTo(1998L)
        assertThat(group.total?.minorUnitsAmount).isEqualTo(2100L)
        val item = group.items.single()
        assertThat(item.displayName).isEqualTo("Llama Figure")
        assertThat(item.quantity).isEqualTo(2)
        assertThat(item.unitAmount?.minorUnitsAmount).isEqualTo(999L)
    }

    @Test
    fun `empty lineItems maps orderSummaryItems to empty list`() {
        val session = createSession(lineItems = emptyList())
        assertThat(session.orderSummaryItems).isEmpty()
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
        assertThat(options[0].amount.minorUnitsAmount).isEqualTo(500L)
        assertThat(options[0].displayName).isEqualTo("Standard Shipping")
        assertThat(options[0].deliveryEstimate).isNull()
        assertThat(options[1].id).isEqualTo("shr_express")
        assertThat(options[1].amount.minorUnitsAmount).isEqualTo(1500L)
        assertThat(options[1].displayName).isEqualTo("Express Shipping")
        assertThat(options[1].deliveryEstimate).isEqualTo("1-3 business days")
    }

    @Test
    fun `empty shippingOptions maps to empty list`() {
        val session = createSession()
        assertThat(session.shippingOptions).isEmpty()
    }

    @Suppress("LongParameterList")
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
        adaptivePricingInfo: CheckoutSessionResponse.AdaptivePricingInfo? = null,
        collectedDetails: CheckoutCollectedDetails = CheckoutCollectedDetails(),
        mode: CheckoutSessionResponse.Mode = CheckoutSessionResponse.Mode.PAYMENT,
        amount: Long = 1000L,
        paymentIntent: PaymentIntent? = null,
        businessName: String? = null,
    ): Session {
        return CheckoutSessionResponseFactory.create(
            id = id,
            status = status,
            liveMode = liveMode,
            currency = currency,
            customerEmail = customerEmail,
            taxStatus = taxStatus,
            totalSummary = totalSummary,
            lineItems = lineItems,
            shippingOptions = shippingOptions,
            adaptivePricingInfo = adaptivePricingInfo,
            mode = mode,
            amount = amount,
            paymentIntent = paymentIntent,
            businessName = businessName,
        ).asCheckoutSession(
            flagImages = null,
            collectedDetails = collectedDetails,
            paymentOptionDisplayData = null,
            availableExpressButtonTypes = emptyList(),
        )
    }
}
