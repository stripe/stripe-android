package com.stripe.android.model.parsers

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.MandateData
import org.json.JSONObject
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class MandateDataJsonParserTest {

    private val parser = MandateDataJsonParser()

    @Test
    fun parse_withCompleteCustomerAcceptance_shouldCreateExpectedObject() {
        val mandateData = requireNotNull(
            parser.parse(CUSTOMER_ACCEPTANCE_COMPLETE_JSON)
        )

        val customerAcceptance = mandateData.customerAcceptance
        assertThat(customerAcceptance.type).isEqualTo("online")

        val online = requireNotNull(customerAcceptance.online)
        assertThat(online.ipAddress).isEqualTo("127.0.0.1")
        assertThat(online.userAgent).isEqualTo("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36")
    }

    @Test
    fun parse_withMinimalCustomerAcceptance_shouldCreateExpectedObject() {
        val mandateData = requireNotNull(
            parser.parse(CUSTOMER_ACCEPTANCE_MINIMAL_JSON)
        )

        val customerAcceptance = mandateData.customerAcceptance
        assertThat(customerAcceptance.type).isEqualTo("online")
        assertThat(customerAcceptance.online).isNull()
    }

    @Test
    fun parse_withNullOnlineDetails_shouldHandleGracefully() {
        val mandateData = requireNotNull(
            parser.parse(CUSTOMER_ACCEPTANCE_NULL_ONLINE_JSON)
        )

        val customerAcceptance = mandateData.customerAcceptance
        assertThat(customerAcceptance.type).isEqualTo("online")
        assertThat(customerAcceptance.online).isNull()
    }

    @Test
    fun parse_withInvalidJson_shouldReturnNull() {
        val mandateData = parser.parse(INVALID_JSON)
        assertThat(mandateData).isNull()
    }

    private companion object {
        val CUSTOMER_ACCEPTANCE_COMPLETE_JSON = JSONObject(
            """
            {
              "customer_acceptance": {
                "online": {
                  "ip_address": "127.0.0.1",
                  "user_agent": "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36"
                },
                "type": "online"
              }
            }
            """.trimIndent()
        )

        val CUSTOMER_ACCEPTANCE_MINIMAL_JSON = JSONObject(
            """
            {
              "customer_acceptance": {
                "type": "online"
              }
            }
            """.trimIndent()
        )

        val CUSTOMER_ACCEPTANCE_NULL_ONLINE_JSON = JSONObject(
            """
            {
              "customer_acceptance": {
                "online": null,
                "type": "online"
              }
            }
            """.trimIndent()
        )

        val INVALID_JSON = JSONObject(
            """
            {
              "invalid_field": "value"
            }
            """.trimIndent()
        )
    }
}