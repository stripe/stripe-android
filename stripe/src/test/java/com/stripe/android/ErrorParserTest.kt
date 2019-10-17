package com.stripe.android

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test class for [ErrorParser].
 */
class ErrorParserTest {

    @Test
    fun parseError_withInvalidRequestError_createsCorrectObject() {
        val (type, message, _, param) = ErrorParser.parseError(RAW_INVALID_REQUEST_ERROR)
        val errorMessage = "The Stripe API is only accessible over HTTPS.  " + "Please see <https://stripe.com/docs> for more information."
        assertEquals(errorMessage, message)
        assertEquals("invalid_request_error", type)
        assertEquals("", param)
    }

    @Test
    fun parseError_withNoErrorMessage_addsInvalidResponseMessage() {
        assertEquals(ErrorParser.MALFORMED, ErrorParser.parseError(RAW_INCORRECT_FORMAT_ERROR))
    }

    @Test
    fun parseError_withAllFields_parsesAllFields() {
        val (type, message, code, param, declineCode, charge) = ErrorParser.parseError(RAW_ERROR_WITH_ALL_FIELDS)
        assertEquals("code_value", code)
        assertEquals("param_value", param)
        assertEquals("charge_value", charge)
        assertEquals("Your card was declined.", message)
        assertEquals("card_declined", declineCode)
        assertEquals("invalid_request_error", type)
    }

    companion object {
        private val RAW_INVALID_REQUEST_ERROR =
            """
            {
                "error": {
                    "message": "The Stripe API is only accessible over HTTPS.  Please see <https://stripe.com/docs> for more information.",
                    "type": "invalid_request_error"
                }
            }
            """.trimIndent()

        private val RAW_INCORRECT_FORMAT_ERROR =
            """
            {
                "message": "The Stripe API is only accessible over HTTPS.  Please see <https://stripe.com/docs> for more information.",
                "type": "invalid_request_error"
            }
            """.trimIndent()

        private val RAW_ERROR_WITH_ALL_FIELDS =
            """
            {
                "error": {
                    "code": "code_value",
                    "param": "param_value",
                    "charge": "charge_value",
                    "decline_code": "card_declined",
                    "message": "Your card was declined.",
                    "type": "invalid_request_error"
                }
            }
            """.trimIndent()
    }
}
