package com.stripe.android.core.networking

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class RequestIdTest {

    @Test
    fun `fromString() with valid value should return expected object`() {
        assertThat(
            RequestId.fromString("req_123")
        ).isEqualTo(
            RequestId("req_123")
        )
    }

    @Test
    fun `fromString() with blank value should return null`() {
        assertThat(
            RequestId.fromString("   ")
        ).isNull()
    }
}
