package com.stripe.android.stripecardscan.payment.card

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PanValidatorTest {

    // --- LuhnPanValidator ---

    @Test
    fun `LuhnPanValidator validates known good Visa PAN`() {
        assertThat(LuhnPanValidator.isValidPan("4242424242424242")).isTrue()
    }

    @Test
    fun `LuhnPanValidator validates known good Amex PAN`() {
        assertThat(LuhnPanValidator.isValidPan("378282246310005")).isTrue()
    }

    @Test
    fun `LuhnPanValidator rejects PAN with incorrect check digit`() {
        assertThat(LuhnPanValidator.isValidPan("4242424242424243")).isFalse()
    }

    @Test
    fun `LuhnPanValidator rejects empty string`() {
        assertThat(LuhnPanValidator.isValidPan("")).isFalse()
    }

    // --- LengthPanValidator ---

    @Test
    fun `LengthPanValidator accepts 16-digit Visa`() {
        assertThat(LengthPanValidator.isValidPan("4242424242424242")).isTrue()
    }

    @Test
    fun `LengthPanValidator accepts 15-digit Amex`() {
        assertThat(LengthPanValidator.isValidPan("378282246310005")).isTrue()
    }

    @Test
    fun `LengthPanValidator rejects unknown IIN prefix`() {
        assertThat(LengthPanValidator.isValidPan("0000000000000000")).isFalse()
    }

    @Test
    fun `LengthPanValidator rejects empty string`() {
        assertThat(LengthPanValidator.isValidPan("")).isFalse()
    }

    // --- CompositePanValidator (via + operator) ---

    @Test
    fun `composite validator passes when both validators pass`() {
        val composite = LuhnPanValidator + LengthPanValidator

        assertThat(composite.isValidPan("4242424242424242")).isTrue()
    }

    @Test
    fun `composite validator fails when Luhn fails`() {
        val composite = LuhnPanValidator + LengthPanValidator

        // Valid length for Visa but bad Luhn check digit
        assertThat(composite.isValidPan("4242424242424243")).isFalse()
    }

    @Test
    fun `composite validator fails when length fails`() {
        val composite = LuhnPanValidator + LengthPanValidator

        // 12-digit Visa is not a valid length
        assertThat(composite.isValidPan("424242424242")).isFalse()
    }
}
