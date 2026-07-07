package com.stripe.android

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.SamsungPayTokenParams
import com.stripe.android.model.TokenFixtures
import com.stripe.android.model.TokenParams
import com.stripe.android.networking.StripeRepository
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.Test

internal class StripeSamsungPayTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val stripeRepository: StripeRepository = mock()
    private val paymentController: PaymentController = mock()
    private val stripe = Stripe(
        stripeRepository = stripeRepository,
        paymentController = paymentController,
        publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
        workContext = testDispatcher,
    )

    @Test
    fun createSamsungPayPaymentMethodSynchronous_createsPaymentMethodFromToken() = runTest {
        whenever(stripeRepository.createToken(any(), any()))
            .thenReturn(Result.success(TokenFixtures.CARD_TOKEN))
        whenever(stripeRepository.createPaymentMethod(any(), any()))
            .thenReturn(Result.success(PaymentMethodFixtures.CARD_PAYMENT_METHOD))

        val token = "serialized_payment_credential"
        val metadata = mapOf("source" to "samsung_pay")
        val idempotencyKey = "payment_method_idempotency_key"
        val paymentMethod = stripe.createSamsungPayPaymentMethodSynchronous(
            token = token,
            billingDetails = PaymentMethodFixtures.BILLING_DETAILS,
            metadata = metadata,
            allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
            idempotencyKey = idempotencyKey,
        )

        assertThat(paymentMethod).isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_METHOD)

        val tokenParamsCaptor = argumentCaptor<TokenParams>()
        val tokenOptionsCaptor = argumentCaptor<ApiRequest.Options>()
        verify(stripeRepository).createToken(tokenParamsCaptor.capture(), tokenOptionsCaptor.capture())
        assertThat(tokenParamsCaptor.firstValue).isInstanceOf(SamsungPayTokenParams::class.java)
        assertThat(tokenParamsCaptor.firstValue.toParamMap()).isEqualTo(
            SamsungPayTokenParams(token).toParamMap()
        )
        assertThat(tokenOptionsCaptor.firstValue.apiKey).isEqualTo(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        assertThat(tokenOptionsCaptor.firstValue.idempotencyKey).isNull()

        val paymentMethodCreateParamsCaptor = argumentCaptor<PaymentMethodCreateParams>()
        val paymentMethodOptionsCaptor = argumentCaptor<ApiRequest.Options>()
        verify(stripeRepository).createPaymentMethod(
            paymentMethodCreateParamsCaptor.capture(),
            paymentMethodOptionsCaptor.capture(),
        )
        assertThat(paymentMethodCreateParamsCaptor.firstValue.toParamMap()).isEqualTo(
            mapOf(
                "type" to "card",
                "billing_details" to PaymentMethodFixtures.BILLING_DETAILS.toParamMap(),
                "card" to mapOf(
                    "token" to TokenFixtures.CARD_TOKEN.id
                ),
                "metadata" to metadata,
                "allow_redisplay" to PaymentMethod.AllowRedisplay.UNSPECIFIED.value,
            )
        )
        assertThat(paymentMethodOptionsCaptor.firstValue.apiKey).isEqualTo(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        assertThat(paymentMethodOptionsCaptor.firstValue.idempotencyKey).isEqualTo(idempotencyKey)
    }
}
