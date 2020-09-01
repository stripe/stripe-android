package com.stripe.android.utils

import com.google.common.truth.Truth.assertThat
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class StripeUrlUtilsTest {
    @Test
    fun `isStripeUrl supports valid urls`() {
        assertThat(StripeUrlUtils.isStripeUrl("https://stripe.com")).isTrue()
    }

    @Test
    fun `isStripeUrl supports subdomains`() {
        assertThat(StripeUrlUtils.isStripeUrl("https://api.stripe.com")).isTrue()
    }

    @Test
    fun `isStripeUrl supports paths`() {
        assertThat(StripeUrlUtils.isStripeUrl("https://hooks.stripe.com/redirect/complete")).isTrue()
    }

    @Test
    fun `isStripeUrl supports query params`() {
        assertThat(StripeUrlUtils.isStripeUrl("https://hooks.stripe.com/redirect/complete?param=false")).isTrue()
    }

    @Test
    fun `isStripeUrl supports port numbers`() {
        assertThat(StripeUrlUtils.isStripeUrl("https://stripe.com:8080")).isTrue()
    }

    @Test
    fun `isStripeUrl does not allow http`() {
        assertThat(StripeUrlUtils.isStripeUrl("http://stripe.com")).isFalse()
    }

    @Test
    fun `isStripeUrl does not allow other domains`() {
        assertThat(StripeUrlUtils.isStripeUrl("https://fakestripe.com")).isFalse()
    }
}
