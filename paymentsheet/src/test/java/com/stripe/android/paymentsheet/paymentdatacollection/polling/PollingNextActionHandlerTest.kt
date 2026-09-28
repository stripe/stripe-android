package com.stripe.android.paymentsheet.paymentdatacollection.polling

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.R
import com.stripe.android.testing.PaymentIntentFactory
import org.junit.Test

internal class PollingNextActionHandlerTest {
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
        return PaymentIntentFactory.create(
            clientSecret = "pi_123_secret_456",
            paymentMethod = PaymentMethod(
                id = "pm_pix",
                created = 1L,
                liveMode = false,
                code = "pix",
                type = PaymentMethod.Type.Pix,
            ),
        ).copy(
            nextActionData = StripeIntent.NextActionData.DisplayPixDetails(
                expiresAt = expiresAt,
                hostedInstructionsUrl = "https://payments.stripe.com/pix/instructions/test",
            )
        )
    }
}
