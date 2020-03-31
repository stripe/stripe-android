package com.stripe.android.model.parsers

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentIntentFixtures
import kotlin.test.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentIntentJsonParserTest {
    @Test
    fun parse_withExpandedPaymentMethod_shouldCreateExpectedObject() {
        val paymentIntent = PaymentIntentJsonParser().parse(
            PaymentIntentFixtures.EXPANDED_PAYMENT_METHOD
        )
        assertThat(paymentIntent?.paymentMethodId)
            .isEqualTo("pm_1GSTxOCRMbs6FrXfYCosDqyr")
        assertThat(paymentIntent?.paymentMethod?.id)
            .isEqualTo("pm_1GSTxOCRMbs6FrXfYCosDqyr")
    }
}
