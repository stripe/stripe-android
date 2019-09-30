package com.stripe.android

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
