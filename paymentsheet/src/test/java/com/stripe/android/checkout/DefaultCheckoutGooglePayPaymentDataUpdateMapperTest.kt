package com.stripe.android.checkout

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.R
import com.stripe.android.common.model.CommonConfigurationFactory
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdate
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayDisplayItemsFactory
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class DefaultCheckoutGooglePayPaymentDataUpdateMapperTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val mapper = DefaultCheckoutGooglePayPaymentDataUpdateMapper(context)

    @Test
    fun `toResponse returns transaction info built from response`() {
        val response = CheckoutSessionResponseFactory.create(
            currency = "usd",
            amount = 1500L,
            merchantCountry = "US",
            paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
        )

        val result = mapper.toResponse(
            configuration = CommonConfigurationFactory.create(),
            response = response,
            paymentDataUpdate = PAYMENT_DATA_UPDATE,
        )

        assertThat(result.error).isNull()
        assertThat(result.newTransactionInfo).isEqualTo(
            expectedTransactionInfo(
                countryCode = "US",
                totalPrice = 1500L,
                transactionId = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.id,
            )
        )
    }

    @Test
    fun `toResponse uses country code from common configuration google pay when present`() {
        val configuration = CommonConfigurationFactory.create(
            googlePay = PaymentSheet.GooglePayConfiguration(
                environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                countryCode = "CA",
            ),
        )

        val result = mapper.toResponse(
            configuration = configuration,
            response = CheckoutSessionResponseFactory.create(merchantCountry = "US"),
            paymentDataUpdate = PAYMENT_DATA_UPDATE,
        )

        assertThat(result.newTransactionInfo).isEqualTo(expectedTransactionInfo(countryCode = "CA"))
    }

    @Test
    fun `toResponse falls back to checkout session merchant country when common configuration has no google pay`() {
        val result = mapper.toResponse(
            configuration = CommonConfigurationFactory.create(googlePay = null),
            response = CheckoutSessionResponseFactory.create(merchantCountry = "GB"),
            paymentDataUpdate = PAYMENT_DATA_UPDATE,
        )

        assertThat(result.newTransactionInfo).isEqualTo(expectedTransactionInfo(countryCode = "GB"))
    }

    @Test
    fun `toResponse returns null country code when neither common configuration nor merchant country are set`() {
        val result = mapper.toResponse(
            configuration = CommonConfigurationFactory.create(googlePay = null),
            response = CheckoutSessionResponseFactory.create(merchantCountry = null),
            paymentDataUpdate = PAYMENT_DATA_UPDATE,
        )

        assertThat(result.newTransactionInfo).isEqualTo(expectedTransactionInfo(countryCode = null))
    }

    @Test
    fun `toResponse uses total summary total amount due over checkout session amount when present`() {
        val response = CheckoutSessionResponseFactory.create(
            amount = 1000L,
            totalSummary = totalSummary(totalAmountDue = 2500L),
        )

        val result = mapper.toResponse(
            configuration = CommonConfigurationFactory.create(),
            response = response,
            paymentDataUpdate = PAYMENT_DATA_UPDATE,
        )

        assertThat(result.newTransactionInfo).isEqualTo(
            expectedTransactionInfo(
                totalPrice = 2500L,
                displayItems = GooglePayDisplayItemsFactory.create(response).map { it.resolve(context) },
            )
        )
    }

    @Test
    fun `toResponse uses checkout session amount when total summary is absent`() {
        val result = mapper.toResponse(
            configuration = CommonConfigurationFactory.create(),
            response = CheckoutSessionResponseFactory.create(
                amount = 1000L,
                totalSummary = null,
            ),
            paymentDataUpdate = PAYMENT_DATA_UPDATE,
        )

        assertThat(result.newTransactionInfo).isEqualTo(expectedTransactionInfo(totalPrice = 1000L))
    }

    @Test
    fun `toResponse uses payment intent id as transaction id`() {
        val result = mapper.toResponse(
            configuration = CommonConfigurationFactory.create(),
            response = CheckoutSessionResponseFactory.create(
                paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                setupIntent = null,
            ),
            paymentDataUpdate = PAYMENT_DATA_UPDATE,
        )

        assertThat(result.newTransactionInfo).isEqualTo(
            expectedTransactionInfo(transactionId = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.id)
        )
    }

    @Test
    fun `toResponse uses setup intent id as transaction id when no payment intent`() {
        val result = mapper.toResponse(
            configuration = CommonConfigurationFactory.create(),
            response = CheckoutSessionResponseFactory.create(
                paymentIntent = null,
                setupIntent = SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT,
            ),
            paymentDataUpdate = PAYMENT_DATA_UPDATE,
        )

        assertThat(result.newTransactionInfo).isEqualTo(
            expectedTransactionInfo(transactionId = SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT.id)
        )
    }

    private fun expectedTransactionInfo(
        countryCode: String? = "US",
        totalPrice: Long? = 1000L,
        transactionId: String? = null,
        displayItems: List<GooglePayJsonFactory.DisplayItem> = emptyList(),
    ) = GooglePayJsonFactory.TransactionInfo(
        currencyCode = "USD",
        totalPriceStatus = GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.Estimated,
        countryCode = countryCode,
        transactionId = transactionId,
        totalPrice = totalPrice,
        totalPriceLabel = context.getString(R.string.stripe_google_pay_total),
        checkoutOption = GooglePayJsonFactory.TransactionInfo.CheckoutOption.Default,
        displayItems = displayItems,
    )

    private fun totalSummary(
        totalAmountDue: Long,
    ) = CheckoutSessionResponse.TotalSummaryResponse(
        subtotal = 1000L,
        totalDueToday = totalAmountDue,
        totalAmountDue = totalAmountDue,
        discountAmounts = emptyList(),
        taxAmounts = emptyList(),
        shippingRate = null,
        appliedBalance = null,
    )

    private companion object {
        val PAYMENT_DATA_UPDATE = GooglePayPaymentDataUpdate(
            callbackTrigger = GooglePayPaymentDataUpdate.CallbackTrigger.Initialize,
            shippingAddress = null,
        )
    }
}
