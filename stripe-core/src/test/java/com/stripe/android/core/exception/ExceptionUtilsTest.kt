package com.stripe.android.core.exception

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.IOException
import java.lang.IllegalArgumentException
import java.lang.NullPointerException

class ExceptionUtilsTest {
    @Test
    fun `'safeAnalyticsMessage' should return 'analyticsValue' result from 'StripeException' subtypes`() {
        assertThat(FakeException.safeAnalyticsMessage).isEqualTo("fakeException")
    }

    @Test
    fun `'safeAnalyticsMessage' should return 'ioException' from 'IOException'`() {
        assertThat(IOException().safeAnalyticsMessage).isEqualTo("ioException")
    }

    @Test
    fun `'safeAnalyticsMessage' should return 'unknown' from other exception types`() {
        assertThat(IllegalArgumentException().safeAnalyticsMessage).isEqualTo("unknown")
        assertThat(NullPointerException().safeAnalyticsMessage).isEqualTo("unknown")
    }

    private object FakeException : StripeException() {
        override fun analyticsValue(): String = "fakeException"
    }
}
