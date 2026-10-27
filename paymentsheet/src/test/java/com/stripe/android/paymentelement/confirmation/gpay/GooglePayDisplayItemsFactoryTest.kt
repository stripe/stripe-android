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
    fun `automatic tax enabled uses tax summary labels even without tax rows`() {
        val response = CheckoutSessionResponseFactory.create(
            automaticTaxEnabled = true,
            checkoutItems = listOf(
                CheckoutSessionResponseFactory.checkoutItem(subtotal = 2000, total = 1950)
            ),
            totalSummary = null,
        )

        val items = GooglePayDisplayItemsFactory.create(response, context)

        assertThat(items.map { it.label }).containsExactly(
            "Widget", "Cost excluding tax", "Estimated total (final tax may vary)"
        ).inOrder()
        assertThat(items.map { it.price }).containsExactly(2000L, 2000L, 1950L).inOrder()
        assertThat(items.map { it.type }).containsExactly(
            GooglePayJsonFactory.DisplayItem.Type.LINE_ITEM,
            GooglePayJsonFactory.DisplayItem.Type.SUBTOTAL,
            GooglePayJsonFactory.DisplayItem.Type.LINE_ITEM,
        ).inOrder()
    }

    @Test
    fun `automatic tax disabled uses subtotal and total labels and preserves taxes`() {
        val tax = CheckoutSessionResponse.TaxAmount(
            amount = 150,
            inclusive = false,
            taxRate = CheckoutSessionResponse.TaxRate(
                displayName = "Sales tax",
                percentage = 8.0,
                rateType = CheckoutSessionResponse.TaxRateType.PERCENTAGE,
            ),
        )
        val response = CheckoutSessionResponseFactory.create(
            automaticTaxEnabled = false,
            checkoutItems = listOf(
                CheckoutSessionResponseFactory.checkoutItem(subtotal = 2000, total = 1950)
            ),
            totalSummary = CheckoutSessionResponse.TotalSummary(
                totalTaxAmounts = listOf(tax),
            ),
        )

        val items = GooglePayDisplayItemsFactory.create(response, context)

        assertThat(items.map { it.label }).containsExactly(
            "Widget", "Subtotal", "Sales tax", "Total"
        ).inOrder()
        assertThat(items.map { it.price }).containsExactly(2000L, 2000L, 150L, 1950L).inOrder()
        assertThat(items.map { it.type }).containsExactly(
            GooglePayJsonFactory.DisplayItem.Type.LINE_ITEM,
            GooglePayJsonFactory.DisplayItem.Type.SUBTOTAL,
            GooglePayJsonFactory.DisplayItem.Type.TAX,
            GooglePayJsonFactory.DisplayItem.Type.LINE_ITEM,
        ).inOrder()
    }

    @Test
    fun `absent aggregate tax produces no tax row`() {
        val response = CheckoutSessionResponseFactory.create(totalSummary = null)

        val items = GooglePayDisplayItemsFactory.create(response, context)

        assertThat(items.none { it.type == GooglePayJsonFactory.DisplayItem.Type.TAX }).isTrue()
    }
}
