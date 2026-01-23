package com.stripe.android.model.parsers

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.AccountRange
import com.stripe.android.model.BinFixtures
import com.stripe.android.model.BinRange
import com.stripe.android.model.CardFunding
import com.stripe.android.model.CardMetadata
import org.json.JSONObject
import kotlin.test.Test

class CardMetadataJsonParserTest {

    @Test
    fun `parse should create expected object`() {
        assertThat(CardMetadataJsonParser(BinFixtures.VISA, isNetwork = true).parse(DEFAULT))
            .isEqualTo(
                CardMetadata(
                    BinFixtures.VISA,
                    listOf(
                        AccountRange(
                            binRange = BinRange(
                                low = "4242420000000000",
                                high = "4242424239999999",
                                isStatic = false
                            ),
                            panLength = 16,
                            brandInfo = AccountRange.BrandInfo.Visa,
                            funding = CardFunding.Unknown,
                            country = "GB"
                        ),
                        AccountRange(
                            binRange = BinRange(
                                low = "4242424250000000",
                                high = "4242429999999999",
                                isStatic = false
                            ),
                            panLength = 16,
                            brandInfo = AccountRange.BrandInfo.Visa,
                            funding = CardFunding.Unknown,
                            country = "GB"
                        )
                    )
                )
            )
    }

    @Test
    fun `parse should drop objects with missing fields`() {
        assertThat(CardMetadataJsonParser(BinFixtures.VISA, isNetwork = true).parse(MISSING_FIELD))
            .isEqualTo(
                CardMetadata(
                    BinFixtures.VISA,
                    listOf(
                        AccountRange(
                            binRange = BinRange(
                                low = "4242420000000000",
                                high = "4242424239999999",
                                isStatic = false
                            ),
                            panLength = 16,
                            brandInfo = AccountRange.BrandInfo.Visa,
                            funding = CardFunding.Unknown,
                            country = "GB"
                        )
                    )
                )
            )
    }

    @Test
    fun `parse should handle empty result`() {
        assertThat(CardMetadataJsonParser(BinFixtures.VISA, isNetwork = true).parse(EMPTY))
            .isEqualTo(
                CardMetadata(
                    BinFixtures.VISA,
                    emptyList()
                )
            )
    }

    @Test
    fun `factory should be called with isNetwork true when isNetwork is true`() {
        var capturedIsNetwork: Boolean? = null
        val fakeFactory = AccountRangeJsonParser.Factory { isNetwork ->
            capturedIsNetwork = isNetwork
            AccountRangeJsonParser(isNetwork)
        }

        CardMetadataJsonParser(BinFixtures.VISA, isNetwork = true, fakeFactory).parse(DEFAULT)

        assertThat(capturedIsNetwork).isTrue()
    }

    @Test
    fun `factory should be called with isNetwork false when isNetwork is false`() {
        var capturedIsNetwork: Boolean? = null
        val fakeFactory = AccountRangeJsonParser.Factory { isNetwork ->
            capturedIsNetwork = isNetwork
            AccountRangeJsonParser(isNetwork)
        }

        CardMetadataJsonParser(BinFixtures.VISA, isNetwork = false, fakeFactory).parse(DEFAULT)

        assertThat(capturedIsNetwork).isFalse()
    }

    private companion object {
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
