package com.stripe.android.stripe3ds2.transaction

import kotlin.test.Test
import kotlin.test.assertEquals

class StripeErrorMessageTest {

    @Test
    fun getters_shouldReturnCorrectValue() {
        val errorMessage = ErrorMessage(
            "trans_id",
            "error_code",
            "error_description",
            "error_details"
        )
        assertEquals("trans_id", errorMessage.transactionId)
        assertEquals("error_code", errorMessage.errorCode)
        assertEquals("error_description", errorMessage.errorDescription)
        assertEquals("error_details", errorMessage.errorDetails)
    }
}
