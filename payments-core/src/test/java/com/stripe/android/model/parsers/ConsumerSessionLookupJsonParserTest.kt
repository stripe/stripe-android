package com.stripe.android.model.parsers

import com.stripe.android.model.ConsumerFixtures
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSessionLookup
import org.junit.Test
import kotlin.test.assertEquals

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
                    verificationSessions = emptyList()
                ),
                errorMessage = null
            )
        )
    }
}
