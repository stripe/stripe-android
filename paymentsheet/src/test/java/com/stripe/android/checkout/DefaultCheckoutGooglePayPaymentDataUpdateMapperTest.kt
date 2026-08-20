package com.stripe.android.checkout

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.R
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
            paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
        )

        val result = mapper.toResponse(
            configuration = googlePayConfiguration(countryCode = "US"),
            response = response,
            paymentDataUpdate = PAYMENT_DATA_UPDATE,
        )

        assertThat(result.error).isNull()

        val transactionInfo = result.newTransactionInfo
        assertThat(transactionInfo).isNotNull()
        assertThat(transactionInfo?.currencyCode).isEqualTo("USD")
        assertThat(transactionInfo?.totalPriceStatus)
            .isEqualTo(GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.Estimated)
        assertThat(transactionInfo?.countryCode).isEqualTo("US")
        assertThat(transactionInfo?.transactionId).isEqualTo(PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.id)
        assertThat(transactionInfo?.totalPrice).isEqualTo(1500L)
        assertThat(transactionInfo?.totalPriceLabel).isEqualTo(context.getString(R.string.stripe_google_pay_total))
        assertThat(transactionInfo?.checkoutOption)
            .isEqualTo(GooglePayJsonFactory.TransactionInfo.CheckoutOption.Default)
    }

    @Test
    fun `toResponse uses country code from configuration`() {
        val result = mapper.toResponse(
            configuration = googlePayConfiguration(countryCode = "CA"),
            response = CheckoutSessionResponseFactory.create(),
            paymentDataUpdate = PAYMENT_DATA_UPDATE,
        )

        assertThat(result.newTransactionInfo?.countryCode).isEqualTo("CA")
    }

    @Test
    fun `toResponse returns null country code when configuration country code is blank`() {
        val result = mapper.toResponse(
            configuration = googlePayConfiguration(countryCode = ""),
            response = CheckoutSessionResponseFactory.create(),
            paymentDataUpdate = PAYMENT_DATA_UPDATE,
        )

        assertThat(result.newTransactionInfo).isNotNull()
        assertThat(result.newTransactionInfo?.countryCode).isNull()
    }

    @Test
    fun `toResponse uses total summary total amount due over checkout session amount when present`() {
        val response = CheckoutSessionResponseFactory.create(
            amount = 1000L,
            totalSummary = totalSummary(totalAmountDue = 2500L),
        )

        val result = mapper.toResponse(
            configuration = googlePayConfiguration(),
            response = response,
            paymentDataUpdate = PAYMENT_DATA_UPDATE,
        )

        assertThat(result.newTransactionInfo?.totalPrice).isEqualTo(2500L)
    }

    @Test
    fun `toResponse uses checkout session amount when total summary is absent`() {
        val result = mapper.toResponse(
            configuration = googlePayConfiguration(),
            response = CheckoutSessionResponseFactory.create(
                amount = 1000L,
                totalSummary = null,
            ),
            paymentDataUpdate = PAYMENT_DATA_UPDATE,
        )

        assertThat(result.newTransactionInfo?.totalPrice).isEqualTo(1000L)
    }

    @Test
    fun `toResponse uses payment intent id as transaction id`() {
        val result = mapper.toResponse(
            configuration = googlePayConfiguration(),
            response = CheckoutSessionResponseFactory.create(
                paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                setupIntent = null,
            ),
            paymentDataUpdate = PAYMENT_DATA_UPDATE,
        )

        assertThat(result.newTransactionInfo?.transactionId)
            .isEqualTo(PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.id)
    }

    @Test
    fun `toResponse uses setup intent id as transaction id when no payment intent`() {
        val result = mapper.toResponse(
            configuration = googlePayConfiguration(),
            response = CheckoutSessionResponseFactory.create(
                paymentIntent = null,
                setupIntent = SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT,
            ),
            paymentDataUpdate = PAYMENT_DATA_UPDATE,
        )

        assertThat(result.newTransactionInfo?.transactionId)
            .isEqualTo(SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT.id)
    }

    @Test
    fun `toResponse uses display items built from response`() {
        val response = CheckoutSessionResponseFactory.create(
            totalSummary = totalSummary(totalAmountDue = 2500L),
        )

        val result = mapper.toResponse(
            configuration = googlePayConfiguration(),
            response = response,
            paymentDataUpdate = PAYMENT_DATA_UPDATE,
        )

        assertThat(result.newTransactionInfo?.displayItems)
            .isEqualTo(GooglePayDisplayItemsFactory.create(response, context))
    }

    private fun googlePayConfiguration(
        countryCode: String = "US",
    ) = PaymentSheet.GooglePayConfiguration(
        environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
        countryCode = countryCode,
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
