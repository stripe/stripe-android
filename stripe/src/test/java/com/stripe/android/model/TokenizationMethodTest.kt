package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class TokenizationMethodTest {

    @Test
    fun `android_pay should map to GooglePay`() {
        assertThat(
            TokenizationMethod.fromCode("android_pay")
        ).isEqualTo(TokenizationMethod.GooglePay)
    }

    @Test
    fun `google should map to GooglePay`() {
        assertThat(
            TokenizationMethod.fromCode("google")
        ).isEqualTo(TokenizationMethod.GooglePay)
    }

    @Test
    fun `apple_pay should map to ApplePay`() {
        assertThat(
            TokenizationMethod.fromCode("apple_pay")
        ).isEqualTo(TokenizationMethod.ApplePay)
    }
}
