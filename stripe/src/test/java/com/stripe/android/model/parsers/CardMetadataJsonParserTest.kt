package com.stripe.android.model.parsers

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.BinRange
import com.stripe.android.model.CardMetadata
import org.json.JSONObject
import org.junit.Test

class CardMetadataJsonParserTest {

    @Test
    fun `parse should create expected object`() {
        assertThat(CardMetadataJsonParser(BIN_PREFIX).parse(DEFAULT))
            .isEqualTo(
                CardMetadata(
                    BIN_PREFIX,
                    listOf(
                        CardMetadata.AccountRange(
                            binRange = BinRange(low = "4242420000000000", high = "4242424239999999"),
                            panLength = 16,
                            brand = "VISA",
                            country = "GB"
                        ),
                        CardMetadata.AccountRange(
                            binRange = BinRange(low = "4242424250000000", high = "4242429999999999"),
                            panLength = 16,
                            brand = "VISA",
                            country = "GB"
                        )
                    )
                )
            )
    }

    @Test
    fun `parse should drop objects with missing fields`() {
        assertThat(CardMetadataJsonParser(BIN_PREFIX).parse(MISSING_FIELD))
            .isEqualTo(
                CardMetadata(
                    BIN_PREFIX,
                    listOf(
                        CardMetadata.AccountRange(
                            binRange = BinRange(low = "4242420000000000", high = "4242424239999999"),
                            panLength = 16,
                            brand = "VISA",
                            country = "GB"
                        )
                    )
                )
            )
    }

    @Test
    fun `parse should handle empty result`() {
        assertThat(CardMetadataJsonParser(BIN_PREFIX).parse(EMPTY))
            .isEqualTo(
                CardMetadata(
                    BIN_PREFIX,
                    emptyList()
                )
            )
    }

    private companion object {
        private const val BIN_PREFIX = "424242"

        private val DEFAULT = JSONObject(
            """
                {
                  "data": [
                    {
                      "account_range_high": "4242424239999999",
                      "account_range_low": "4242420000000000",
                      "pan_length": 16,
                      "brand": "VISA",
                      "country": "GB"
                    },
                    {
                      "account_range_high": "4242429999999999",
                      "account_range_low": "4242424250000000",
                      "pan_length": 16,
                      "brand": "VISA",
                      "country": "GB"
                    }
                  ]
                }
            """.trimIndent()
        )

        private val MISSING_FIELD = JSONObject(
            """
                {
                  "data": [
                    {
                      "account_range_high": "4242424239999999",
                      "account_range_low": "4242420000000000",
                      "pan_length": 16,
                      "brand": "VISA",
                      "country": "GB"
                    },
                    {
                      "account_range_high": "4242429999999999",
                      "pan_length": 16,
                      "brand": "VISA",
                      "country": "GB"
                    }
                  ]
                }
            """.trimIndent()
        )

        private val EMPTY = JSONObject(
            """{data:[]}"""
        )
    }
}
