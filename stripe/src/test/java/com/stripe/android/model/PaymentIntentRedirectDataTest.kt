package com.stripe.android.model

import android.net.Uri
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentIntentRedirectDataTest {

    @Test
    fun create_withBothFieldsPopulated_shouldReturnCorrectObject() {
        val url = "https://example.com"
        val returnUrl = "yourapp://post-authentication-return-url"
        val redirectMap = mapOf(
            StripeIntent.RedirectData.FIELD_URL to url,
            StripeIntent.RedirectData.FIELD_RETURN_URL to returnUrl
        )

        val redirectData =
            requireNotNull(StripeIntent.RedirectData.create(redirectMap))
        assertEquals(Uri.parse(url), redirectData.url)
        assertEquals(returnUrl, redirectData.returnUrl)
    }

    @Test
    fun create_withOnlyUrlFieldPopulated_shouldReturnCorrectObject() {
        val url = "https://example.com"
        val redirectMap = mapOf(StripeIntent.RedirectData.FIELD_URL to url)

        val redirectData = StripeIntent.RedirectData.create(redirectMap)
        assertNotNull(redirectData)
        assertEquals(Uri.parse(url), redirectData.url)
        assertNull(redirectData.returnUrl)
    }

    @Test
    fun create_withInvalidData_shouldReturnNull() {
        assertNull(StripeIntent.RedirectData.create(emptyMap<String, String>()))
    }
}
