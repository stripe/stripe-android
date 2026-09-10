package com.stripe.android.paymentelement.confirmation.gpay

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GooglePayDisplayItemsFactoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `flattens nested price items using server computed subtotal`() {
        val response = CheckoutSessionResponseFactory.create(
            checkoutItems = listOf(
                CheckoutSessionResponseFactory.checkoutItem(
                    name = "Widget",
                    quantity = 2,
                    unitAmount = 1000,
                    subtotal = 1800,
                    total = 1950,
                )
            )
        )

        val items = GooglePayDisplayItemsFactory.create(response, context)

        assertThat(items.first().label).isEqualTo("Widget x2")
        assertThat(items.first().price).isEqualTo(1800)
        assertThat(items.first().type).isEqualTo(GooglePayJsonFactory.DisplayItem.Type.LINE_ITEM)
    }

    @Test
    fun `uses unified aggregates for subtotal discounts taxes and final total`() {
        val tax = CheckoutSessionResponse.TaxAmount(
            amount = 150,
            inclusive = false,
            taxRate = CheckoutSessionResponse.TaxRate(
                displayName = "Sales tax",
                percentage = 8.0,
                rateType = CheckoutSessionResponse.TaxRateType.PERCENTAGE,
            ),
        )
        val discount = CheckoutSessionResponse.DiscountAmount(
            amount = 200,
            displayName = "Summer",
            coupon = CheckoutSessionResponse.Coupon("SUMMER", null, 10.0),
            promotionCode = null,
        )
        val response = CheckoutSessionResponseFactory.create(
            checkoutItems = listOf(
                CheckoutSessionResponseFactory.checkoutItem(subtotal = 2000, total = 1950)
            ),
            recurringDetails = CheckoutSessionResponse.RecurringDetails(
                totalDiscountAmounts = listOf(discount),
                totalTaxAmounts = listOf(tax),
            ),
        )

        val items = GooglePayDisplayItemsFactory.create(response, context)

        assertThat(items.map { it.price }).containsAtLeast(2000L, -200L, 150L, 1950L)
        assertThat(items.map { it.label }).containsAtLeast("Summer", "Sales tax")
    }

    @Test
    fun `absent aggregate tax produces no tax row`() {
        val response = CheckoutSessionResponseFactory.create(recurringDetails = null)

        val items = GooglePayDisplayItemsFactory.create(response, context)

        assertThat(items.none { it.type == GooglePayJsonFactory.DisplayItem.Type.TAX }).isTrue()
    }
}
