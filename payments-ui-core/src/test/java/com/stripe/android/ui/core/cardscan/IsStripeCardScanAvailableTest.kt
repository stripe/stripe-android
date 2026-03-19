package com.stripe.android.ui.core.cardscan

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IsStripeCardScanAvailableTest {

    @Test
    fun `returns true when CardScanSheet class is on classpath`() {
        val isAvailable = DefaultIsStripeCardScanAvailable()

        // CardScanSheet is on the classpath during tests because stripecardscan is a compileOnly dependency
        assertThat(isAvailable()).isTrue()
    }

    @Test
    fun `returns false when class is not on classpath`() {
        val isAvailable = IsStripeCardScanAvailable {
            try {
                Class.forName("com.stripe.android.stripecardscan.nonexistent.FakeClass")
                true
            } catch (_: ClassNotFoundException) {
                false
            }
        }

        assertThat(isAvailable()).isFalse()
    }
}
