package com.stripe.android.common.taptoadd

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DefaultStripeTerminalVersionValidatorTest {
    private val validator = DefaultStripeTerminalVersionValidator()

    @Test
    fun `returns true for 5-4-1`() {
        assertThat(validator("5.4.1")).isTrue()
    }

    @Test
    fun `returns false for 5-4-0`() {
        assertThat(validator("5.4.0")).isFalse()
    }

    @Test
    fun `returns true for 5-4-1 and higher patch on minor version 4`() {
        assertThat(validator("5.4.2")).isTrue()
        assertThat(validator("5.4.99")).isTrue()
    }

    @Test
    fun `returns true for 5-5-0 when minor is above minimum`() {
        assertThat(validator("5.5.0")).isTrue()
        assertThat(validator("5.10.0")).isTrue()
    }

    @Test
    fun `returns false when major version is below 5`() {
        assertThat(validator("4.4.1")).isFalse()
    }

    @Test
    fun `returns false when minor version is below 4`() {
        assertThat(validator("5.3.9")).isFalse()
    }

    @Test
    fun `returns false when version does not have three segments`() {
        assertThat(validator("5.4")).isFalse()
        assertThat(validator("5")).isFalse()
        assertThat(validator("5.4.1.0")).isFalse()
        assertThat(validator("")).isFalse()
    }

    @Test
    fun `returns false when numeric segments are invalid and coerce to zero`() {
        assertThat(validator("a.4.1")).isFalse()
        assertThat(validator("5.b.1")).isFalse()
        assertThat(validator("5.4.b")).isFalse()
    }
}
