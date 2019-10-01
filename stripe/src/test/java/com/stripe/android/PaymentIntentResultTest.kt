package com.stripe.android

import com.stripe.android.model.PaymentIntentFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentIntentResultTest {

    @Test
    fun testBuilder() {
        assertEquals(PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            PaymentIntentResult.Builder()
                .setPaymentIntent(PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2)
                .build()
                .intent)
    }
}
