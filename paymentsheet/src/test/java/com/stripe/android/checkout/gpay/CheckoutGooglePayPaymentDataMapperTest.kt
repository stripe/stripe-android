package com.stripe.android.checkout.gpay

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.GooglePayConfig
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.checkout.CheckoutControllerStateFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.repositories.TotalSummaryResponseFactory
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class CheckoutGooglePayPaymentDataMapperTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `includes display items in transaction info update`() {
        val checkoutSessionResponse = CheckoutSessionResponseFactory.create(
            lineItems = listOf(
                CheckoutSessionResponse.LineItem(
                    id = "li_1",
                    name = "Widget",
                    quantity = 1,
                    unitAmount = 2000L,
                    subtotal = 2000L,
                    total = 2000L,
                ),
            ),
            totalSummary = TotalSummaryResponseFactory.create(
                subtotal = 2000L,
                totalDueToday = 2500L,
                totalAmountDue = 2500L,
                taxAmounts = listOf(
                    CheckoutSessionResponse.TaxAmount(
                        amount = 500L,
                        inclusive = false,
                        displayName = "Sales Tax",
                        percentage = 10.0,
                    ),
                ),
            ),
        )
        val state = CheckoutControllerStateFactory.create(
            checkoutSessionResponse = checkoutSessionResponse,
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                checkoutSessionResponse = checkoutSessionResponse,
            ),
        )

        val transactionInfo = CheckoutGooglePayPaymentDataMapper.createTransactionInfo(
            state = state,
            context = context,
        )

        val paymentDataRequest = GooglePayJsonFactory(
            googlePayConfig = GooglePayConfig("pk_test"),
        ).createPaymentDataRequest(
            transactionInfo = transactionInfo,
            merchantInfo = GooglePayJsonFactory.MerchantInfo(merchantName = "Test"),
        )
        val displayItems = paymentDataRequest
            .getJSONObject("transactionInfo")
            .getJSONArray("displayItems")

        assertThat(displayItems.length()).isEqualTo(4)
        assertThat(displayItems.getJSONObject(0).getString("label")).isEqualTo("Widget")
        assertThat(displayItems.getJSONObject(2).getString("type")).isEqualTo("TAX")
        assertThat(paymentDataRequest.getJSONObject("transactionInfo").getString("totalPrice"))
            .isEqualTo("25.00")
    }

    @Test
    fun `maps checkout session shipping options for Google Pay`() {
        val response = CheckoutSessionResponseFactory.create(
            totalSummary = TotalSummaryResponseFactory.create(
                shippingRate = CheckoutSessionResponse.ShippingRate(
                    id = "shr_express",
                    amount = 1500L,
                    displayName = "Express Shipping",
                    deliveryEstimate = "1-3 business days",
                ),
            ),
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

        val shippingOptions = CheckoutGooglePayPaymentDataMapper.createShippingOptionParameters(response)

        assertThat(shippingOptions?.defaultSelectedOptionId).isEqualTo("shr_express")
        assertThat(shippingOptions?.shippingOptions).hasSize(2)
        assertThat(shippingOptions?.shippingOptions?.first()?.id).isEqualTo("shr_standard")
        assertThat(shippingOptions?.shippingOptions?.last()?.description).isEqualTo("1-3 business days")
    }

    @Test
    fun `returns null shipping options when checkout session has none`() {
        val response = CheckoutSessionResponseFactory.create(shippingOptions = emptyList())

        assertThat(CheckoutGooglePayPaymentDataMapper.createShippingOptionParameters(response)).isNull()
    }
}
