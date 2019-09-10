package com.stripe.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UidParamsFactoryTest {

    @Test
    fun testCreate() {
        val uidParams = UidParamsFactory("com.app", FakeUidSupplier())
            .createParams()
        assertEquals(2, uidParams.size.toLong())
        assertTrue(uidParams.containsKey("muid"))
        assertTrue(uidParams.containsKey("guid"))
    }
}
