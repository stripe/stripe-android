package com.stripe.android

import kotlin.test.Test
import kotlin.test.assertEquals
import org.json.JSONObject

/**
 * Test class for [StripeErrorJsonParser].
 */
class StripeErrorJsonParserTest {

    @Test
    fun parseError_withInvalidRequestError_createsCorrectObject() {
        val actual = StripeErrorJsonParser().parse(RAW_INVALID_REQUEST_ERROR)
        val expected = StripeError(
            message = "The Stripe API is only accessible over HTTPS. Please see <https://stripe.com/docs> for more information.",
            type = "invalid_request_error"
        )
        assertEquals(expected, actual)
    }

    @Test
    fun parseError_withEmptyJson_addsInvalidResponseMessage() {
        assertEquals(
            StripeErrorJsonParser.MALFORMED_RESPONSE_MESSAGE,
            StripeErrorJsonParser().parse(JSONObject()).message
        )
    }

    @Test
    fun parseError_withNoErrorMessage_addsInvalidResponseMessage() {
        assertEquals(
            StripeErrorJsonParser.MALFORMED_RESPONSE_MESSAGE,
            StripeErrorJsonParser().parse(RAW_INCORRECT_FORMAT_ERROR).message
        )
    }

    @Test
    fun parseError_withAllFields_parsesAllFields() {
        val expected = StripeError(
            code = "code_value",
            param = "param_value",
            charge = "charge_value",
            message = "Your card was declined.",
            declineCode = "card_declined",
            type = "invalid_request_error"
        )
        assertEquals(
            expected,
            StripeErrorJsonParser().parse(RAW_ERROR_WITH_ALL_FIELDS)
        )
    }

    @Test
    fun parse_withDocUrl() {
        assertEquals(
            "https://stripe.com/docs/error-codes/incorrect-number",
            StripeErrorFixtures.INVALID_CARD_NUMBER.docUrl
        )
    }

    private companion object {
        private val RAW_INVALID_REQUEST_ERROR = JSONObject(
            """
            {
                "error": {
                    "message": "The Stripe API is only accessible over HTTPS. Please see <https://stripe.com/docs> for more information.",
                    "type": "invalid_request_error"
                }
            }
            """.trimIndent()
        )

        private val RAW_INCORRECT_FORMAT_ERROR = JSONObject(
            """
            {
                "message": "The Stripe API is only accessible over HTTPS. Please see <https://stripe.com/docs> for more information.",
                "type": "invalid_request_error"
            }
            """.trimIndent()
        )

        private val RAW_ERROR_WITH_ALL_FIELDS = JSONObject(
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
        )
    }
}
