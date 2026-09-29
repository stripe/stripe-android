package com.stripe.android.paymentsheet.paymentdatacollection.polling

import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.R
import com.stripe.android.testing.PaymentIntentFactory
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
internal class PollingNextActionHandlerTest {
    @Test
    fun `existing payment method keeps polling arguments`(
        @TestParameter(valuesProvider = ExistingPaymentMethodCaseProvider::class)
        testCase: ExistingPaymentMethodCase,
    ) {
        val requestOptions = ApiRequest.Options("pk_test_123")

        val args = PollingNextActionHandler().getArgsForPaymentMethod(
            actionable = createIntent(testCase.paymentMethodType),
            statusBarColor = 0x123456,
            requestOptions = requestOptions,
            currentTimeMillis = 1_700_000_000_000,
        )

        assertThat(args.clientSecret).isEqualTo("pi_123_secret_456")
        assertThat(args.statusBarColor).isEqualTo(0x123456)
        assertThat(args.timeLimitInSeconds).isEqualTo(testCase.timeLimitInSeconds)
        assertThat(args.initialDelayInSeconds).isEqualTo(5)
        assertThat(args.pollingIntervalInSeconds).isEqualTo(1)
        assertThat(args.ctaText).isEqualTo(testCase.ctaText)
        assertThat(args.qrCodeUrl).isNull()
        assertThat(args.paymentMethodType).isEqualTo(testCase.paymentMethodType.code)
        assertThat(args.requestOptions).isSameInstanceAs(requestOptions)
    }

    @Test
    fun `Pix uses hosted instructions and expiration time`() {
        val requestOptions = ApiRequest.Options("pk_test_123")

        val args = PollingNextActionHandler().getArgsForPaymentMethod(
            actionable = createPixIntent(expiresAt = 1_700_003_600),
            statusBarColor = 0x123456,
            requestOptions = requestOptions,
            currentTimeMillis = 1_700_000_000_000,
        )

        assertThat(args.clientSecret).isEqualTo("pi_123_secret_456")
        assertThat(args.statusBarColor).isEqualTo(0x123456)
        assertThat(args.timeLimitInSeconds).isEqualTo(60 * 60)
        assertThat(args.initialDelayInSeconds).isEqualTo(0)
        assertThat(args.pollingIntervalInSeconds).isEqualTo(2)
        assertThat(args.ctaText).isEqualTo(R.string.stripe_pix_confirm_payment)
        assertThat(args.qrCodeUrl).isEqualTo("https://payments.stripe.com/pix/instructions/test")
        assertThat(args.paymentMethodType).isEqualTo("pix")
        assertThat(args.requestOptions).isSameInstanceAs(requestOptions)
    }

    @Test
    fun `Pix falls back to 24 hours when expiration time is absent`() {
        val args = PollingNextActionHandler().getArgsForPaymentMethod(
            actionable = createPixIntent(expiresAt = null),
            statusBarColor = null,
            requestOptions = ApiRequest.Options("pk_test_123"),
            currentTimeMillis = 1_700_000_000_000,
        )

        assertThat(args.timeLimitInSeconds).isEqualTo(24 * 60 * 60)
    }

    @Test
    fun `Pix with an expired QR code has no polling time remaining`() {
        val args = PollingNextActionHandler().getArgsForPaymentMethod(
            actionable = createPixIntent(expiresAt = 1_699_999_999),
            statusBarColor = null,
            requestOptions = ApiRequest.Options("pk_test_123"),
            currentTimeMillis = 1_700_000_000_000,
        )

        assertThat(args.timeLimitInSeconds).isEqualTo(0)
    }

    private fun createPixIntent(expiresAt: Long?): StripeIntent {
        return createIntent(PaymentMethod.Type.Pix).copy(
            nextActionData = StripeIntent.NextActionData.DisplayPixDetails(
                expiresAt = expiresAt,
                hostedInstructionsUrl = "https://payments.stripe.com/pix/instructions/test",
            )
        )
    }

    private fun createIntent(paymentMethodType: PaymentMethod.Type): PaymentIntent {
        return PaymentIntentFactory.create(
            clientSecret = "pi_123_secret_456",
            paymentMethod = PaymentMethod(
                id = "pm_123",
                created = 1L,
                liveMode = false,
                code = paymentMethodType.code,
                type = paymentMethodType,
            ),
        )
    }
}

internal object ExistingPaymentMethodCaseProvider : TestParameterValuesProvider() {
    override fun provideValues(
        context: Context?,
    ): List<ExistingPaymentMethodCase> = listOf(
        ExistingPaymentMethodCase(
            name = "Bizum",
            paymentMethodType = PaymentMethod.Type.Bizum,
            timeLimitInSeconds = 70 * 60,
            ctaText = R.string.stripe_bizum_confirm_payment,
        ),
        ExistingPaymentMethodCase(
            name = "MB WAY",
            paymentMethodType = PaymentMethod.Type.MbWay,
            timeLimitInSeconds = 4 * 60,
            ctaText = R.string.stripe_mb_way_confirm_payment,
        ),
    )
}

internal data class ExistingPaymentMethodCase(
    val name: String,
    val paymentMethodType: PaymentMethod.Type,
    val timeLimitInSeconds: Int,
    val ctaText: Int,
) {
    override fun toString(): String = name
}
