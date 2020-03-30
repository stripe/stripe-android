package com.stripe.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
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
        assertThat(paymentMethod)
            .isNotNull()
    }
}
