package com.stripe.android.model.parsers

import com.google.common.truth.Truth.assertThat
import com.stripe.android.ConsumerFixtures
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.LinkBrand
import junit.framework.TestCase.assertEquals
import org.json.JSONObject
import org.junit.Test

class ConsumerSessionLookupJsonParserTest {

    @Test
    fun `Parse lookup when consumer does not exist`() {
        assertEquals(
            ConsumerSessionLookupJsonParser().parse(ConsumerFixtures.NO_EXISTING_CONSUMER_JSON),
            ConsumerSessionLookup(
                exists = false,
                consumerSession = null,
                errorMessage = "No consumer found for the given email address."
            )
        )
    }

    @Test
    fun `Parse lookup when consumer exists`() {
        assertEquals(
            ConsumerSessionLookupJsonParser().parse(ConsumerFixtures.EXISTING_CONSUMER_JSON),
            ConsumerSessionLookup(
                exists = true,
                consumerSession = ConsumerSession(
                    clientSecret = "secret",
                    emailAddress = "email@example.com",
                    redactedPhoneNumber = "+1********68",
                    redactedFormattedPhoneNumber = "(***) *** **68",
                    unredactedPhoneNumber = null,
                    phoneNumberCountry = null,
                    verificationSessions = emptyList(),
                ),
                errorMessage = null,
                publishableKey = "asdfg123",
            )
        )
    }

    @Test
    fun `link_brand is parsed as Link`() {
        val result = ConsumerSessionLookupJsonParser().parse(createLookupJsonWithLinkBrand("link"))
        assertThat(result.linkBrand).isEqualTo(LinkBrand.Link)
    }

    @Test
    fun `unknown link_brand defaults to Link`() {
        val result = ConsumerSessionLookupJsonParser().parse(createLookupJsonWithLinkBrand("some_future_brand"))
        assertThat(result.linkBrand).isEqualTo(LinkBrand.Link)
    }

    @Test
    fun `missing link_brand is parsed as null`() {
        val result = ConsumerSessionLookupJsonParser().parse(createLookupJsonWithLinkBrand(linkBrand = null))
        assertThat(result.linkBrand).isNull()
    }

    private fun createLookupJsonWithLinkBrand(linkBrand: String?): JSONObject {
        val linkBrandField = if (linkBrand != null) """"link_brand": "$linkBrand",""" else ""
        return JSONObject(
            """
            {
              $linkBrandField
              "publishable_key": "asdfg123",
              "consumer_session": {
                "client_secret": "secret",
                "email_address": "email@example.com",
                "redacted_phone_number": "+1********68",
                "redacted_formatted_phone_number": "(***) *** **68",
                "verification_sessions": []
              },
              "error_message": null,
              "exists": true
            }
            """.trimIndent()
        )
    }
}
