package com.stripe.android.paymentelement.confirmation.gpay

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GooglePayDisplayItemsFactoryTest {

    @Test
    fun `returns empty list when checkoutSessionResponse is null`() {
        val metadata = PaymentMethodMetadataFactory.create(checkoutSessionResponse = null)

        val result = GooglePayDisplayItemsFactory.create(metadata)

        assertThat(result).isEmpty()
    }

    @Test
    fun `returns empty list when checkout session has no line items`() {
        val result = createAndGetDisplayItems(lineItems = emptyList())

        assertThat(result).isEmpty()
    }

    @Test
    fun `maps checkout session line items to display items`() {
        val result = createAndGetDisplayItems(
            lineItems = listOf(
                lineItem(
                    id = "li_1", name = "Widget", quantity = 2,
                    unitAmount = 1000L, subtotal = 2000L, total = 2000L,
                ),
                lineItem(
                    id = "li_2", name = "Gadget", quantity = 1,
                    unitAmount = 500L, subtotal = 500L, total = 450L,
                ),
            ),
        )

        assertThat(result).containsExactly(
            displayItem(
                label = "Widget x2",
                type = GooglePayJsonFactory.DisplayItem.Type.LINE_ITEM,
                price = 1000L,
            ),
            displayItem(
                label = "Gadget",
                type = GooglePayJsonFactory.DisplayItem.Type.LINE_ITEM,
                price = 500L,
            ),
        ).inOrder()
    }

    @Test
    fun `uses total when line item has no unit amount`() {
        val result = createAndGetDisplayItems(
            lineItems = listOf(
                lineItem(
                    id = "li_1", name = "Widget", quantity = 1,
                    unitAmount = null, subtotal = 2000L, total = 1800L,
                ),
            ),
        )

        assertThat(result).containsExactly(
            displayItem(
                label = "Widget",
                type = GooglePayJsonFactory.DisplayItem.Type.LINE_ITEM,
                price = 1800L,
            ),
        )
    }

    @Test
    fun `includes discount amounts from total summary`() {
        val result = createAndGetDisplayItems(
            lineItems = listOf(
                lineItem(
                    id = "li_1", name = "Widget", quantity = 1,
                    unitAmount = 2000L, subtotal = 2000L, total = 2000L,
                ),
            ),
            totalSummary = totalSummary(
                subtotal = 2000L,
                totalDueToday = 1500L,
                totalAmountDue = 1500L,
                discountAmounts = listOf(discountAmount(amount = 500L, displayName = "SAVE50")),
            ),
        )

        assertThat(result).containsExactly(
            displayItem(
                label = "Widget",
                type = GooglePayJsonFactory.DisplayItem.Type.LINE_ITEM,
                price = 2000L,
            ),
            displayItem(
                label = "SAVE50",
                type = GooglePayJsonFactory.DisplayItem.Type.DISCOUNT,
                price = -500L,
            ),
        ).inOrder()
    }

    @Test
    fun `includes tax amounts from total summary`() {
        val result = createAndGetDisplayItems(
            lineItems = listOf(
                lineItem(
                    id = "li_1", name = "Widget", quantity = 1,
                    unitAmount = 1000L, subtotal = 1000L, total = 1000L,
                ),
            ),
            totalSummary = totalSummary(
                subtotal = 1000L,
                totalDueToday = 1080L,
                totalAmountDue = 1080L,
                taxAmounts = listOf(
                    taxAmount(amount = 80L, displayName = "Sales Tax", percentage = 8.0),
                ),
            ),
        )

        assertThat(result).containsExactly(
            displayItem(
                label = "Widget",
                type = GooglePayJsonFactory.DisplayItem.Type.LINE_ITEM,
                price = 1000L,
            ),
            displayItem(
                label = "Sales Tax",
                type = GooglePayJsonFactory.DisplayItem.Type.TAX,
                price = 80L,
            ),
        ).inOrder()
    }

    @Test
    fun `includes both discounts and taxes from total summary`() {
        val result = createAndGetDisplayItems(
            lineItems = listOf(
                lineItem(
                    id = "li_1", name = "Widget", quantity = 1,
                    unitAmount = 2000L, subtotal = 2000L, total = 2000L,
                ),
            ),
            totalSummary = totalSummary(
                subtotal = 2000L,
                totalDueToday = 1620L,
                totalAmountDue = 1620L,
                discountAmounts = listOf(discountAmount(amount = 500L, displayName = "SAVE50")),
                taxAmounts = listOf(taxAmount(amount = 120L, displayName = "VAT", percentage = 8.0)),
            ),
        )

        assertThat(result).containsExactly(
            displayItem(
                label = "Widget",
                type = GooglePayJsonFactory.DisplayItem.Type.LINE_ITEM,
                price = 2000L,
            ),
            displayItem(
                label = "SAVE50",
                type = GooglePayJsonFactory.DisplayItem.Type.DISCOUNT,
                price = -500L,
            ),
            displayItem(
                label = "VAT",
                type = GooglePayJsonFactory.DisplayItem.Type.TAX,
                price = 120L,
            ),
        ).inOrder()
    }

    @Test
    fun `qualified automatic tax billing combines exclusive taxes into estimated presentation`() {
        val presentation = createCheckoutPresentation(
            amount = 1300L,
            taxAmounts = listOf(
                taxAmount(amount = 100L, displayName = "State tax", percentage = 10.0),
                taxAmount(amount = 50L, displayName = "Local tax", percentage = 5.0),
            ),
        )

        assertThat(presentation.displayItems).containsExactly(
            displayItem(
                label = "Cost excluding tax",
                type = GooglePayJsonFactory.DisplayItem.Type.SUBTOTAL,
                price = 1150L,
            ),
            displayItem(
                label = "Estimated tax",
                type = GooglePayJsonFactory.DisplayItem.Type.TAX,
                price = 150L,
            ),
        ).inOrder()
        assertThat(presentation.customLabel).isEqualTo("Estimated total (final tax may vary)")
        assertThat(presentation.totalPriceStatus)
            .isEqualTo(GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.Estimated)
        assertThat(presentation.forceBillingAddressCollection).isTrue()
    }

    @Test
    fun `estimated presentation subtracts only exclusive tax in mixed tax summary`() {
        val presentation = createCheckoutPresentation(
            amount = 1200L,
            taxAmounts = listOf(
                taxAmount(amount = 200L, displayName = "VAT", percentage = 20.0, inclusive = true),
                taxAmount(amount = 50L, displayName = "Local tax", percentage = 5.0),
            ),
        )

        assertThat(presentation.displayItems).containsExactly(
            displayItem("Cost excluding tax", GooglePayJsonFactory.DisplayItem.Type.SUBTOTAL, 1150L),
            displayItem("Estimated tax", GooglePayJsonFactory.DisplayItem.Type.TAX, 50L),
        ).inOrder()
        assertThat(presentation.totalPriceStatus)
            .isEqualTo(GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.Estimated)
    }

    @Test
    fun `zero exclusive tax entry still uses estimated presentation`() {
        val presentation = createCheckoutPresentation(
            amount = 1000L,
            taxAmounts = listOf(
                taxAmount(amount = 0L, displayName = "Sales tax", percentage = 0.0),
            ),
        )

        assertThat(presentation.displayItems).containsExactly(
            displayItem("Cost excluding tax", GooglePayJsonFactory.DisplayItem.Type.SUBTOTAL, 1000L),
            displayItem("Estimated tax", GooglePayJsonFactory.DisplayItem.Type.TAX, 0L),
        ).inOrder()
        assertThat(presentation.totalPriceStatus)
            .isEqualTo(GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.Estimated)
    }

    @Test
    fun `all inclusive or empty tax summary uses final presentation`() {
        listOf(
            listOf(taxAmount(amount = 200L, displayName = "VAT", percentage = 20.0, inclusive = true)),
            emptyList(),
        ).forEach { taxAmounts ->
            val presentation = createCheckoutPresentation(amount = 1000L, taxAmounts = taxAmounts)

            assertThat(presentation.totalPriceStatus)
                .isEqualTo(GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.Final)
            assertThat(presentation.customLabel).isNull()
            assertThat(presentation.forceBillingAddressCollection).isTrue()
        }
    }

    private fun createAndGetDisplayItems(
        lineItems: List<CheckoutSessionResponse.LineItem>,
        totalSummary: CheckoutSessionResponse.TotalSummaryResponse? = null,
    ): List<GooglePayJsonFactory.DisplayItem> {
        val metadata = PaymentMethodMetadataFactory.create(
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(
                lineItems = lineItems,
                totalSummary = totalSummary,
            ),
        )

        return GooglePayDisplayItemsFactory.create(metadata)
    }

    private fun createCheckoutPresentation(
        amount: Long,
        taxAmounts: List<CheckoutSessionResponse.TaxAmount>,
    ): GooglePayDisplayItemsFactory.Presentation {
        val metadata = PaymentMethodMetadataFactory.create(
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(
                amount = amount,
                automaticTaxEnabled = true,
                taxAddressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
                taxStatus = CheckoutSessionResponse.TaxStatus.READY,
                totalSummary = totalSummary(
                    subtotal = amount,
                    totalDueToday = amount,
                    totalAmountDue = amount,
                    taxAmounts = taxAmounts,
                ),
            ),
        )
        return GooglePayDisplayItemsFactory.createCheckoutPresentation(
            resources = ApplicationProvider.getApplicationContext<Context>().resources,
            paymentMethodMetadata = metadata,
            hasQualifiedDefaultBillingTaxEstimate = true,
        )
    }

    private companion object {
        fun lineItem(
            id: String,
            name: String,
            quantity: Int,
            unitAmount: Long?,
            subtotal: Long,
            total: Long,
        ) = CheckoutSessionResponse.LineItem(
            id = id,
            name = name,
            quantity = quantity,
            unitAmount = unitAmount,
            subtotal = subtotal,
            total = total,
        )

        fun totalSummary(
            subtotal: Long,
            totalDueToday: Long,
            totalAmountDue: Long,
            discountAmounts: List<CheckoutSessionResponse.DiscountAmount> = emptyList(),
            taxAmounts: List<CheckoutSessionResponse.TaxAmount> = emptyList(),
        ) = CheckoutSessionResponse.TotalSummaryResponse(
            subtotal = subtotal,
            totalDueToday = totalDueToday,
            totalAmountDue = totalAmountDue,
            discountAmounts = discountAmounts,
            taxAmounts = taxAmounts,
            shippingRate = null,
            appliedBalance = null,
        )

        fun discountAmount(amount: Long, displayName: String) = CheckoutSessionResponse.DiscountAmount(
            amount = amount,
            displayName = displayName,
        )

        fun taxAmount(
            amount: Long,
            displayName: String,
            percentage: Double,
            inclusive: Boolean = false,
        ) = CheckoutSessionResponse.TaxAmount(
            amount = amount,
            inclusive = inclusive,
            displayName = displayName,
            percentage = percentage,
        )

        fun displayItem(
            label: String,
            type: GooglePayJsonFactory.DisplayItem.Type,
            price: Long,
        ) = GooglePayJsonFactory.DisplayItem(
            label = label,
            type = type,
            price = price,
        )
    }
}
