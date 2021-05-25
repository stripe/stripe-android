package com.stripe.android.cards

import com.google.common.truth.Truth.assertThat
import com.stripe.android.CardNumberFixtures
import com.stripe.android.model.BinFixtures
import kotlin.test.Test

class BinTest {
    @Test
    fun `create() with 2 digit partial card number should return null`() {
        assertThat(
            Bin.create("42")
        ).isNull()
    }

    @Test
    fun `create() with 6 digit partial card number should return BIN`() {
        assertThat(
            BinFixtures.VISA
        ).isEqualTo(
            Bin(DEFAULT_BIN)
        )
    }

    @Test
    fun `create() with full card number should return BIN`() {
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
