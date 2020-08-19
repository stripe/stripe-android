package com.stripe.android.cards

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class CardNumberTest {

    @Test
    fun `getFormatted() with panLength 16 with empty number`() {
        assertThat(
            CardNumber.Unvalidated("   ").getFormatted(16)
        ).isEqualTo("")
    }

    @Test
    fun `getFormatted() with panLength 16 with 3 digit number`() {
        assertThat(
            CardNumber.Unvalidated("4 2 4").getFormatted(16)
        ).isEqualTo("424")
    }

    @Test
    fun `getFormatted() with panLength 16 with full number`() {
        assertThat(
            CardNumber.Unvalidated("42424242 42424242").getFormatted(16)
        ).isEqualTo("4242 4242 4242 4242")
    }

    @Test
    fun `getFormatted() with panLength 16 and extraneous digits should format and remove extraneous digits`() {
        assertThat(
            CardNumber.Unvalidated("42424242 42424242 42424").getFormatted(16)
        ).isEqualTo("4242 4242 4242 4242")
    }

    @Test
    fun `getFormatted() with panLength 19 with partial number`() {
        assertThat(
            CardNumber.Unvalidated("6216828050").getFormatted(19)
        ).isEqualTo("6216 8280 50")
    }

    @Test
    fun `getFormatted() with panLength 19 with full number`() {
        assertThat(
            CardNumber.Unvalidated("6216828050123456789").getFormatted(19)
        ).isEqualTo("6216 8280 5012 3456 789")
    }

    @Test
    fun `getFormatted() with panLength 14 with partial number`() {
        assertThat(
            CardNumber.Unvalidated("3622720").getFormatted(14)
        ).isEqualTo("3622 720")
    }

    @Test
    fun `getFormatted() with panLength 14 with full number`() {
        assertThat(
            CardNumber.Unvalidated("36227206271667").getFormatted(14)
        ).isEqualTo("3622 720627 1667")
    }
}
