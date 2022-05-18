package com.stripe.android.stripecardscan.framework.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SmallTest
import com.stripe.android.stripecardscan.R
import org.junit.Test
import kotlin.test.assertEquals

class StringTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    @SmallTest
    fun testStringNotChanged() {
        val privacyString =
            context.resources.getString(R.string.stripe_card_scan_privacy_link_text)

        val expected = "We use Stripe to verify your card details. Stripe may use and store your" +
            " data according its privacy policy. " +
            "<a href=https://support.stripe.com/questions/stripes-card-image-verification>" +
            "<u>Learn more</u></a>"

        assertEquals(expected, privacyString)
    }
}
