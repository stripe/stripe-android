package com.stripe.android.cards

import com.google.common.truth.Truth.assertThat
import com.stripe.android.CardNumberFixtures
import kotlin.test.Test

class BinTest {
    @Test
    fun `create() should return expected value`() {
        assertThat(
            Bin.create("42")
        ).isNull()

        assertThat(
            Bin.create("424242")
        ).isEqualTo(
            Bin(DEFAULT_BIN)
        )

        assertThat(
            Bin.create(CardNumberFixtures.VISA_NO_SPACES)
        ).isEqualTo(
            Bin(DEFAULT_BIN)
        )
    }

    private companion object {
        private const val DEFAULT_BIN = "424242"
    }
}
