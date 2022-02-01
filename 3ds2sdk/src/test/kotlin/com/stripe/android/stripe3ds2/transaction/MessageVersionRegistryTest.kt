package com.stripe.android.stripe3ds2.transaction

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MessageVersionRegistryTest {

    private val mMessageVersionRegistry = MessageVersionRegistry()

    @Test
    fun getCurrent_shouldReturnCurrentVersion() {
        assertEquals("2.1.0", mMessageVersionRegistry.current)
    }

    @Test
    fun isSupported_withSupportedVersion_shouldReturnTrue() {
        assertTrue(mMessageVersionRegistry.isSupported("2.1.0"))
    }

    @Test
    fun isSupported_withUnsupportedVersion_shouldReturnFalse() {
        assertFalse(mMessageVersionRegistry.isSupported("0.0.0"))
    }
}
