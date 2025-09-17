package com.stripe.android.model.parsers

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmationTokenFixtures
import org.json.JSONObject
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class ConfirmationTokenJsonParserTest {

    private val parser = ConfirmationTokenJsonParser()

    @Test
    fun parse_withCompleteJson_shouldCreateExpectedObject() {
        val confirmationToken = requireNotNull(
            parser.parse(ConfirmationTokenFixtures.CONFIRMATION_TOKEN_JSON)
        )

        assertThat(confirmationToken.id).isEqualTo("ctoken_1NnQUf2eZvKYlo2CIObdtbnb")
        assertThat(confirmationToken.created).isEqualTo(1694025025L)
        assertThat(confirmationToken.expiresAt).isEqualTo(1694068225L)
        assertThat(confirmationToken.liveMode).isTrue()
        assertThat(confirmationToken.mandateData).isNull()
        assertThat(confirmationToken.paymentIntentId).isNull()
        assertThat(confirmationToken.returnUrl).isEqualTo("https://example.com/return")
        assertThat(confirmationToken.setupFutureUsage).isEqualTo(ConfirmPaymentIntentParams.SetupFutureUsage.OffSession)
        assertThat(confirmationToken.setupIntentId).isNull()
    }

    @Test
    fun parse_withCompleteJson_shouldParsePaymentMethodPreview() {
        val confirmationToken = requireNotNull(
            parser.parse(ConfirmationTokenFixtures.CONFIRMATION_TOKEN_JSON)
        )

        val paymentMethodPreview = requireNotNull(confirmationToken.paymentMethodPreview)
        assertThat(paymentMethodPreview.type).isEqualTo(com.stripe.android.model.PaymentMethod.Type.Card)
        assertThat(paymentMethodPreview.customerId).isNull()
        assertThat(paymentMethodPreview.allowRedisplay).isNull()

        val billingDetails = requireNotNull(paymentMethodPreview.billingDetails)
        assertThat(billingDetails.name).isEqualTo("Jenny Rosen")
        assertThat(billingDetails.email).isEqualTo("jennyrosen@stripe.com")

        val address = requireNotNull(billingDetails.address)
        assertThat(address.line1).isEqualTo("50 Sprague St")
        assertThat(address.city).isEqualTo("Hyde Park")
        assertThat(address.state).isEqualTo("MA")
        assertThat(address.postalCode).isEqualTo("02136")
        assertThat(address.country).isEqualTo("US")

        // Verify allResponseFields contains the original JSON
        assertThat(paymentMethodPreview.allResponseFields).isNotEmpty()
        assertThat(paymentMethodPreview.allResponseFields).contains("\"type\":\"card\"")
    }

    @Test
    fun parse_withCompleteJson_shouldParseShipping() {
        val confirmationToken = requireNotNull(
            parser.parse(ConfirmationTokenFixtures.CONFIRMATION_TOKEN_JSON)
        )

        val shipping = requireNotNull(confirmationToken.shipping)
        assertThat(shipping.name).isEqualTo("Jenny Rosen")
        assertThat(shipping.phone).isNull()

        val address = requireNotNull(shipping.address)
        assertThat(address.line1).isEqualTo("50 Sprague St")
        assertThat(address.city).isEqualTo("Hyde Park")
        assertThat(address.state).isEqualTo("MA")
        assertThat(address.postalCode).isEqualTo("02136")
        assertThat(address.country).isEqualTo("US")
    }

    @Test
    fun parse_withMinimalFields_shouldCreateExpectedObject() {
        val confirmationToken = requireNotNull(
            parser.parse(ConfirmationTokenFixtures.MINIMAL_CONFIRMATION_TOKEN_JSON)
        )

        assertThat(confirmationToken.id).isEqualTo("ctoken_1234567890")
        assertThat(confirmationToken.created).isEqualTo(1694025025L)
        assertThat(confirmationToken.liveMode).isFalse()
        assertThat(confirmationToken.expiresAt).isNull()
        assertThat(confirmationToken.mandateData).isNull()
        assertThat(confirmationToken.paymentIntentId).isNull()
        assertThat(confirmationToken.paymentMethodPreview).isNull()
        assertThat(confirmationToken.returnUrl).isNull()
        assertThat(confirmationToken.setupFutureUsage).isNull()
        assertThat(confirmationToken.setupIntentId).isNull()
        assertThat(confirmationToken.shipping).isNull()
    }

    @Test
    fun parse_withMissingId_shouldReturnNull() {
        val confirmationToken = parser.parse(ConfirmationTokenFixtures.CONFIRMATION_TOKEN_WITHOUT_ID_JSON)
        assertThat(confirmationToken).isNull()
    }

    @Test
    fun parse_withMissingCreated_shouldReturnNull() {
        val confirmationToken = parser.parse(ConfirmationTokenFixtures.CONFIRMATION_TOKEN_WITHOUT_CREATED_JSON)
        assertThat(confirmationToken).isNull()
    }

    @Test
    fun parse_withInvalidSetupFutureUsage_shouldIgnoreInvalidValue() {
        val confirmationToken = requireNotNull(
            parser.parse(ConfirmationTokenFixtures.CONFIRMATION_TOKEN_WITH_INVALID_SETUP_FUTURE_USAGE_JSON)
        )

        assertThat(confirmationToken.setupFutureUsage).isNull()
    }

    @Test
    fun parse_withEmptyStringId_shouldReturnNull() {
        val jsonWithEmptyId = JSONObject("""{"id": "", "created": 1694025025}""")
        val result = parser.parse(jsonWithEmptyId)
        assertThat(result).isNull()
    }

    @Test
    fun parse_withInvalidPaymentMethodPreview_shouldHandleGracefully() {
        val invalidPaymentMethodJson = JSONObject(
            """
            {
                "id": "ctoken_test", 
                "created": 1694025025,
                "payment_method_preview": {"invalid": "data"}
            }
            """.trimIndent()
        )
        val result = parser.parse(invalidPaymentMethodJson)
        assertThat(result?.paymentMethodPreview).isNull()
    }
}
