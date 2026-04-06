package com.stripe.android.stripecardscan.cardscan

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CardScanSheetTest {

    @Test
    fun `isSupported returns false when Google Play Services is unavailable`() {
        val result = CardScanSheet.isSupported(ApplicationProvider.getApplicationContext())
        assertThat(result).isFalse()
    }
}
