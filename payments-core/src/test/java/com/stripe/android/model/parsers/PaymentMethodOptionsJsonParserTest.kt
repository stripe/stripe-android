package com.stripe.android.model.parsers

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethodOptions
import org.json.JSONObject
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class PaymentMethodOptionsJsonParserTest {

    private val parser = PaymentMethodOptionsJsonParser()

    @Test
    fun parse_withCardCvcToken_shouldCreateExpectedObject() {
        val paymentMethodOptions = requireNotNull(
            parser.parse(CARD_WITH_CVC_TOKEN_JSON)
        )

        val card = requireNotNull(paymentMethodOptions.card)
        assertThat(card.cvcToken).isEqualTo("cvctok_1234567890")
    }

    @Test
    fun parse_withCardNullCvcToken_shouldCreateExpectedObject() {
        val paymentMethodOptions = requireNotNull(
            parser.parse(CARD_WITH_NULL_CVC_TOKEN_JSON)
        )

        val card = requireNotNull(paymentMethodOptions.card)
        assertThat(card.cvcToken).isNull()
    }

    @Test
    fun parse_withEmptyCardOptions_shouldCreateExpectedObject() {
        val paymentMethodOptions = requireNotNull(
            parser.parse(EMPTY_CARD_OPTIONS_JSON)
        )

        val card = requireNotNull(paymentMethodOptions.card)
        assertThat(card.cvcToken).isNull()
    }

    @Test
    fun parse_withNullCard_shouldCreateExpectedObject() {
        val paymentMethodOptions = requireNotNull(
            parser.parse(NULL_CARD_JSON)
        )

        assertThat(paymentMethodOptions.card).isNull()
    }

    @Test
    fun parse_withEmptyJson_shouldCreateExpectedObject() {
        val paymentMethodOptions = requireNotNull(
            parser.parse(EMPTY_JSON)
        )

        assertThat(paymentMethodOptions.card).isNull()
    }

    private companion object {
        val CARD_WITH_CVC_TOKEN_JSON = JSONObject(
            """
            {
              "card": {
                "cvc_token": "cvctok_1234567890"
              }
            }
            """.trimIndent()
        )

        val CARD_WITH_NULL_CVC_TOKEN_JSON = JSONObject(
            """
            {
              "card": {
                "cvc_token": null
              }
            }
            """.trimIndent()
        )

        val EMPTY_CARD_OPTIONS_JSON = JSONObject(
            """
            {
              "card": {}
            }
            """.trimIndent()
        )

        val NULL_CARD_JSON = JSONObject(
            """
            {
              "card": null
            }
            """.trimIndent()
        )

        val EMPTY_JSON = JSONObject(
            """
            {}
            """.trimIndent()
        )
    }
}