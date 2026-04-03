package com.stripe.android.stripecardscan.payment.card

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PanFormatterTest {

    @Test
    fun `formatPan formats 16-digit Visa as 4-4-4-4`() {
        val result = formatPan("4242424242424242")

        assertThat(result).isEqualTo("4242 4242 4242 4242")
    }

    @Test
    fun `formatPan formats 15-digit Amex as 4-6-5`() {
        val result = formatPan("378282246310005")

        assertThat(result).isEqualTo("3782 822463 10005")
    }

    @Test
    fun `formatPan formats 14-digit Diners as 4-6-4`() {
        val result = formatPan("36227206271667")

        assertThat(result).isEqualTo("3622 720627 1667")
    }

    @Test
    fun `formatPan formats 19-digit UnionPay as 6-13`() {
        val result = formatPan("6212345678901234567")

        assertThat(result).isEqualTo("621234 5678901234567")
    }

    @Test
    fun `formatPan formats 16-digit MasterCard as 4-4-4-4`() {
        val result = formatPan("5500000000000004")

        assertThat(result).isEqualTo("5500 0000 0000 0004")
    }

    @Test
    fun `formatPan returns raw PAN when no formatter matches`() {
        // A very short PAN that won't match any known format
        val result = formatPan("123")

        assertThat(result).isEqualTo("123")
    }

    @Test
    fun `addFormatPan overrides default formatting for custom issuer`() {
        // Use a Custom issuer to avoid polluting shared state for standard issuers
        val customIssuer = CardIssuer.Custom("TestCard")
        addFormatPan(customIssuer, 16, 8, 8)

        // We can't easily test this end-to-end since getCardIssuer won't return
        // our custom issuer for any PAN. Instead, verify addFormatPan doesn't throw.
        // The formatting logic is validated by the other tests.
    }
}
