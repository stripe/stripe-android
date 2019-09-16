package com.stripe.android.model

import org.junit.Assert.assertEquals
import org.junit.Test

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
