package com.stripe.android

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class UidParamsFactoryTest {

    @Test
    fun testCreate() {
        val uidParams = UidParamsFactory("com.app", FakeUidSupplier())
            .createParams()
        assertThat(uidParams.keys)
            .containsExactly("muid", "guid")
    }
}
