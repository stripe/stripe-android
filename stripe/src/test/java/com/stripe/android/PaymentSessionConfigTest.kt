package com.stripe.android

import com.stripe.android.PaymentSessionFixtures.PAYMENT_SESSION_CONFIG
import com.stripe.android.utils.ParcelUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentSessionConfigTest {

    @Test
    fun testParcel() {
        assertEquals(PAYMENT_SESSION_CONFIG, ParcelUtils.create(PAYMENT_SESSION_CONFIG))
    }
}
