package com.stripe.android.model.parsers

import com.google.common.truth.Truth.assertThat
import com.stripe.android.FingerprintData
import kotlin.test.Test
import org.json.JSONObject

class FingerprintDataJsonParserTest {
    @Test
    fun `should parse correctly`() {
        assertThat(FingerprintDataJsonParser { 100L }.parse(JSON))
            .isEqualTo(
                FingerprintData(
                    guid = "200",
                    muid = "100",
                    sid = "300",
                    timestamp = 100L
                )
            )
    }

    private companion object {
        private val JSON = JSONObject(
            """
                {
                    "guid": "200",
                    "muid": "100",
                    "sid": "300"
                }
            """.trimIndent()
        )
    }
}
