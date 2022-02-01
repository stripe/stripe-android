package com.stripe.android.stripe3ds2.transactions

import org.json.JSONException
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ErrorDataTest {

    @Test
    @Throws(JSONException::class)
    fun fromJson_shouldReturnExpectedErrorData() {
        val errorData = ErrorDataFixtures.ERROR_DATA

        assertNotNull(UUID.fromString(errorData.serverTransId))
        assertNotNull(UUID.fromString(errorData.acsTransId))
        assertNotNull(UUID.fromString(errorData.dsTransId))
        assertNotNull(errorData.sdkTransId)

        assertEquals("Data Decryption Failure", errorData.errorCode)
        assertEquals("D", errorData.errorComponent?.code)
        assertEquals("Data could not be decrypted by the receiving system due to technical or other reason.", errorData.errorDescription)
        assertEquals("Description of the failure.", errorData.errorDetail)
        assertEquals("CReq", errorData.errorMessageType)

        assertEquals("2.2.0", errorData.messageVersion)
    }

    @Test
    fun testJsonParsingRoundtrip() {
        assertEquals(
            ErrorDataFixtures.ERROR_DATA,
            ErrorData.fromJson(ErrorDataFixtures.ERROR_DATA.toJson())
        )
    }
}
