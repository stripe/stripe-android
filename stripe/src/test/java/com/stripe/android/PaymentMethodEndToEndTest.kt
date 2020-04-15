package com.stripe.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import kotlin.test.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentMethodEndToEndTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun createPaymentMethod_withBacsDebit_shouldCreateObject() {
        val params = PaymentMethodCreateParamsFixtures.BACS_DEBIT

        val paymentMethod =
            Stripe(context, ApiKeyFixtures.BACS_PUBLISHABLE_KEY)
                .createPaymentMethodSynchronous(params)
        assertThat(paymentMethod?.type)
            .isEqualTo(PaymentMethod.Type.BacsDebit)
        assertThat(paymentMethod?.bacsDebit)
            .isEqualTo(
                PaymentMethod.BacsDebit(
                    fingerprint = "UkSG0HfCGxxrja1H",
                    last4 = "2345",
                    sortCode = "108800"
                )
            )
    }

    @Test
    fun createPaymentMethod_withSofort_shouldCreateObject() {
        val params = PaymentMethodCreateParamsFixtures.SOFORT
        val paymentMethod =
            Stripe(context, ApiKeyFixtures.SOFORT_PUBLISHABLE_KEY)
                .createPaymentMethodSynchronous(params)
        assertThat(paymentMethod?.type)
            .isEqualTo(PaymentMethod.Type.Sofort)
        assertThat(paymentMethod?.sofort)
            .isEqualTo(
                PaymentMethod.Sofort(
                    country = "DE"
                )
            )
    }

    @Test
    fun createPaymentMethod_withP24_shouldCreateObject() {
        val params = PaymentMethodCreateParamsFixtures.P24
        val paymentMethod =
            Stripe(context, ApiKeyFixtures.P24_PUBLISHABLE_KEY)
                .createPaymentMethodSynchronous(params)
        assertThat(paymentMethod?.type)
            .isEqualTo(PaymentMethod.Type.P24)
    }
}
