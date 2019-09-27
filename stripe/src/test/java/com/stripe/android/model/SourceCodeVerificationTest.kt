package com.stripe.android.model

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test class for [SourceCodeVerification].
 */
class SourceCodeVerificationTest {

    @Test
    fun fromJsonString_createsObject() {
        val codeVerification = SourceFixtures.SOURCE_CODE_VERIFICATION
        assertEquals(3, codeVerification.attemptsRemaining.toLong())
        assertEquals(SourceCodeVerification.Status.PENDING, codeVerification.status)
    }
}
