package com.stripe.android.model.parsers

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethod
import org.json.JSONObject
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class PaymentMethodPreviewJsonParserTest {

    private val parser = PaymentMethodPreviewJsonParser()

    @Test
    fun parse_withCompleteJson_shouldCreateExpectedObject() {
        val json = JSONObject(
            """
            {
                "type": "card",
                "billing_details": {
                    "address": {
                        "city": "Hyde Park",
                        "country": "US",
                        "line1": "50 Sprague St",
                        "line2": "",
                        "postal_code": "02136",
                        "state": "MA"
                    },
                    "email": "jennyrosen@stripe.com",
                    "name": "Jenny Rosen",
                    "phone": null
                },
                "customer": "cus_test123",
                "allow_redisplay": "always"
            }
            """.trimIndent()
        )

        val paymentMethodPreview = requireNotNull(parser.parse(json))

        assertThat(paymentMethodPreview.type).isEqualTo(PaymentMethod.Type.Card)
        assertThat(paymentMethodPreview.customerId).isEqualTo("cus_test123")
        assertThat(paymentMethodPreview.allowRedisplay).isEqualTo(PaymentMethod.AllowRedisplay.ALWAYS)
        
        val billingDetails = requireNotNull(paymentMethodPreview.billingDetails)
        assertThat(billingDetails.name).isEqualTo("Jenny Rosen")
        assertThat(billingDetails.email).isEqualTo("jennyrosen@stripe.com")
        
        val address = requireNotNull(billingDetails.address)
        assertThat(address.line1).isEqualTo("50 Sprague St")
        assertThat(address.city).isEqualTo("Hyde Park")
        assertThat(address.state).isEqualTo("MA")
        assertThat(address.postalCode).isEqualTo("02136")
        assertThat(address.country).isEqualTo("US")

        assertThat(paymentMethodPreview.allResponseFields).contains("\"type\":\"card\"")
    }

    @Test
    fun parse_withMinimalJson_shouldCreateExpectedObject() {
        val json = JSONObject("""{"type": "card"}""")

        val paymentMethodPreview = requireNotNull(parser.parse(json))

        assertThat(paymentMethodPreview.type).isEqualTo(PaymentMethod.Type.Card)
        assertThat(paymentMethodPreview.customerId).isNull()
        assertThat(paymentMethodPreview.allowRedisplay).isNull()
        assertThat(paymentMethodPreview.billingDetails).isNull()
        assertThat(paymentMethodPreview.allResponseFields).isEqualTo("""{"type":"card"}""")
    }

    @Test
    fun parse_withMissingType_shouldReturnNull() {
        val json = JSONObject(
            """
            {
                "billing_details": {
                    "name": "Jenny Rosen"
                }
            }
            """.trimIndent()
        )

        val result = parser.parse(json)
        assertThat(result).isNull()
    }

    @Test
    fun parse_withInvalidAllowRedisplay_shouldIgnoreInvalidValue() {
        val json = JSONObject(
            """
            {
                "type": "card",
                "allow_redisplay": "invalid_value"
            }
            """.trimIndent()
        )

        val paymentMethodPreview = requireNotNull(parser.parse(json))
        assertThat(paymentMethodPreview.allowRedisplay).isNull()
    }

    @Test
    fun parse_withUnknownType_shouldReturnNull() {
        val json = JSONObject("""{"type": "unknown_type"}""")

        val result = parser.parse(json)
        assertThat(result).isNull()
    }
}