package com.stripe.android.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Test class for [SourceReceiver].
 */
class SourceReceiverTest {

    @Test
    fun fromJson_createsExpectedObject() {
        val sourceReceiver = SourceFixtures.SOURCE_RECEIVER
        assertEquals("test_1MBhWS3uv4ynCfQXF3xQjJkzFPukr4K56N", sourceReceiver.address)
        assertEquals(10, sourceReceiver.amountCharged)
        assertEquals(20, sourceReceiver.amountReceived)
        assertEquals(30, sourceReceiver.amountReturned)
    }
}
