package com.stripe.android.model.parsers

import com.stripe.android.model.SourceTypeModel
import org.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SourceSepaDebitDataJsonParserTest {
    @Test
    fun fromJson_withExampleData_returnsExpectedObject() {
        val sepaData = parse(EXAMPLE_SEPA_JSON_DATA)
        assertEquals("37040044", sepaData.bankCode)
        assertEquals("R8MJxzkSUv1Kv07L", sepaData.fingerPrint)
        assertEquals("CK4K2GFVPOVR4T5B", sepaData.mandateReference)
        assertEquals("DE", sepaData.country)
        assertEquals("3000", sepaData.last4)
        assertEquals(MANDATE_URL, sepaData.mandateUrl)
        assertNull(sepaData.branchCode)
    }

    @Test
    fun testEquals() {
        assertEquals(
            parse(EXAMPLE_SEPA_JSON_DATA),
            parse(EXAMPLE_SEPA_JSON_DATA)
        )
    }

    @Test
    fun testHashCode() {
        assertEquals(
            parse(EXAMPLE_SEPA_JSON_DATA).hashCode(),
            parse(EXAMPLE_SEPA_JSON_DATA).hashCode()
        )
    }

    private fun parse(jsonObject: JSONObject): SourceTypeModel.SepaDebit {
        return SourceSepaDebitDataJsonParser().parse(jsonObject)
    }

    @Suppress("MaxLineLength")
    private companion object {
        private val EXAMPLE_SEPA_JSON_DATA = JSONObject(
            """
            {
                "bank_code": "37040044",
                "country": "DE",
                "fingerprint": "R8MJxzkSUv1Kv07L",
                "last4": "3000",
                "mandate_reference": "CK4K2GFVPOVR4T5B",
                "mandate_url": "https:\/\/hooks.stripe.com\/adapter\/sepa_debit\/file\/src_1A0burBbvEcIpqUbyTfDmJPk\/src_client_secret_5Dgw1AQGTABOh0vlnKyxgboh"
            }
            """.trimIndent()
        )

        private const val MANDATE_URL = "https://hooks.stripe.com/adapter/sepa_debit/file/" +
            "src_1A0burBbvEcIpqUbyTfDmJPk/src_client_secret_5Dgw1AQGTABOh0vlnKyxgboh"
    }
}
