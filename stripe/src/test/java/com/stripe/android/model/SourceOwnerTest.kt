package com.stripe.android.model

import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Test class for [SourceOwner] model.
 */
class SourceOwnerTest {
    @Test
    fun fromJsonStringWithoutNulls_isNotNull() {
        assertNotNull(SourceOwner.fromJson(SourceFixtures.SOURCE_OWNER_WITHOUT_NULLS))
    }

    @Test
    fun fromJsonStringWithNulls_IsNotNull() {
        assertNotNull(SourceOwner.fromJson(SourceFixtures.SOURCE_OWNER_WITH_NULLS))
    }
}
