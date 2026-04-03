package com.stripe.android.core.model.parsers

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.StripeError
import com.stripe.android.core.StripeErrorFixtures
import org.json.JSONObject
import kotlin.test.Test

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
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun parseError_withEmptyJson_addsInvalidResponseMessage() {
        assertThat(StripeErrorJsonParser().parse(JSONObject()).message)
            .isEqualTo(StripeErrorJsonParser.MALFORMED_RESPONSE_MESSAGE)
    }

    @Test
    fun parseError_withNoErrorMessage_addsInvalidResponseMessage() {
        assertThat(StripeErrorJsonParser().parse(RAW_INCORRECT_FORMAT_ERROR).message)
            .isEqualTo(StripeErrorJsonParser.MALFORMED_RESPONSE_MESSAGE)
    }

    @Test
    fun parseError_withAllFields_parsesAllFields() {
        val expected = StripeError(
            code = "code_value",
            param = "param_value",
            charge = "charge_value",
            message = "Your card was declined.",
            declineCode = "card_declined",
            type = "invalid_request_error",
            extraFields = mapOf(
                "test_number" to "1",
                "test_boolean" to "true",
                "test_string" to "hola"
            )
        )
        assertThat(StripeErrorJsonParser().parse(RAW_ERROR_WITH_ALL_FIELDS))
            .isEqualTo(expected)
    }

    @Test
    fun parse_withDocUrl() {
        assertThat(StripeErrorFixtures.INVALID_CARD_NUMBER.docUrl)
            .isEqualTo("https://stripe.com/docs/error-codes/incorrect-number")
    }

    @Suppress("MaxLineLength")
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
                    "type": "invalid_request_error",
                    "extra_fields": {
                        "test_number": 1, 
                        "test_boolean": true,
                        "test_string": "hola" 
                    }
                }
            }
            """.trimIndent()
        )
    }
}
