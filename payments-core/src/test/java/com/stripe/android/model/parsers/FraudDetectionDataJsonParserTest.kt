package com.stripe.android.model.parsers

import com.google.common.truth.Truth.assertThat
import com.stripe.android.networking.FraudDetectionData
import org.json.JSONObject
import kotlin.test.Test

class FraudDetectionDataJsonParserTest {
    @Test
    fun `should parse correctly`() {
        assertThat(FraudDetectionDataJsonParser { 100L }.parse(JSON))
            .isEqualTo(
                FraudDetectionData(
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
