package com.stripe.android.model

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Test class for [SourceSepaDebitData].
 */
class SourceSepaDebitDataTest {

    @Test
    fun fromJson_withExampleData_returnsExpectedObject() {
        val sepaData = SourceSepaDebitData.fromJson(EXAMPLE_SEPA_JSON_DATA)!!
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
            SourceSepaDebitData.fromJson(EXAMPLE_SEPA_JSON_DATA),
            SourceSepaDebitData.fromJson(EXAMPLE_SEPA_JSON_DATA)
        )
    }

    @Test
    fun testHashCode() {
        assertEquals(
            SourceSepaDebitData.fromJson(EXAMPLE_SEPA_JSON_DATA)!!.hashCode().toLong(),
            SourceSepaDebitData.fromJson(EXAMPLE_SEPA_JSON_DATA)!!.hashCode().toLong()
        )
    }

    companion object {
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

        private const val MANDATE_URL =
            "https://hooks.stripe.com/adapter/sepa_debit/file/src_1A0burBbvEcIpqUbyTfDmJPk/src_client_secret_5Dgw1AQGTABOh0vlnKyxgboh"
    }
}
