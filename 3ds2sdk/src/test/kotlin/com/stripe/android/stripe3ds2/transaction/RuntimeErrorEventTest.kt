package com.stripe.android.stripe3ds2.transaction

import com.google.common.truth.Truth.assertThat
import com.stripe.android.stripe3ds2.exceptions.SDKRuntimeException
import kotlin.test.Test

class RuntimeErrorEventTest {

    @Test
    fun testImplementation() {
        assertThat(
            RuntimeErrorEvent(
                SDKRuntimeException("epic fail")
            )
        ).isEqualTo(
            RuntimeErrorEvent(
                "SDKRuntimeException",
                "epic fail"
            )
        )
    }
}
