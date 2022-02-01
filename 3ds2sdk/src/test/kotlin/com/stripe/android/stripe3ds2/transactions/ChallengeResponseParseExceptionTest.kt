package com.stripe.android.stripe3ds2.transactions

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class ChallengeResponseParseExceptionTest {

    @Test
    fun testGetMessage() {
        val message = ChallengeResponseParseException
            .createInvalidDataElementFormat("sdkTransID")
            .message
        assertThat(message)
            .isEqualTo(
                "203 - Data element not in the required format or value is invalid as defined in Table A.1 (sdkTransID)"
            )
    }
}
