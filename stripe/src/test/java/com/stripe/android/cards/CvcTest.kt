package com.stripe.android.cards

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class CvcTest {

    @Test
    fun `validate() with empty string should return null`() {
        assertThat(
            Cvc.Unvalidated("  ").validate(3)
        ).isNull()
    }

    @Test
    fun `validate() with non-digits string should return null`() {
        assertThat(
            Cvc.Unvalidated("abc").validate(4)
        ).isNull()
    }

    @Test
    fun `validate() with valid digits including spaces should return validated CVC`() {
        assertThat(
            Cvc.Unvalidated("1 2 3").validate(4)
        ).isEqualTo(
            Cvc.Validated("123")
        )
    }

    @Test
    fun `validate() with valid digits string should return validated CVC`() {
        assertThat(
            Cvc.Unvalidated("123").validate(4)
        ).isEqualTo(
            Cvc.Validated("123")
        )
    }

    @Test
    fun `validate() with valid digits string longer than max length should return null`() {
        assertThat(
            Cvc.Unvalidated("1234").validate(3)
        ).isNull()
    }
}
