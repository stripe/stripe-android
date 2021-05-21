package com.stripe.android.model.parsers

import com.google.common.truth.Truth.assertThat
import com.stripe.android.cards.AccountRangeFixtures
import kotlin.test.Test

class AccountRangeJsonParserTest {

    private val accountRangeJsonParser = AccountRangeJsonParser()

    @Test
    fun `serialize-deserialize roundtrip should return expected object`() {
        val accountRange = AccountRangeFixtures.DEFAULT.first()

        assertThat(
            accountRangeJsonParser.parse(
                accountRangeJsonParser.serialize(accountRange)
            )
        ).isEqualTo(accountRange)
    }
}
