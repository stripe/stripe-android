package com.stripe.android

import com.google.common.truth.Truth.assertThat
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
class StripeClientTest {

    @Test
    fun `valid publishable key is accepted`() {
        val client = StripeClient(publishableKey = "pk_test_123")
        assertThat(client.publishableKey).isEqualTo("pk_test_123")
    }

    @Test
    fun `secret key throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            StripeClient(publishableKey = "sk_test_123")
        }
    }

    @Test
    fun `blank key throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            StripeClient(publishableKey = "")
        }
    }

    @Test
    fun `stripeAccountId is null by default`() {
        val client = StripeClient(publishableKey = "pk_test_123")
        assertThat(client.stripeAccountId).isNull()
    }

    @Test
    fun `stripeAccountId round-trips when set`() {
        val client = StripeClient(publishableKey = "pk_test_123", stripeAccountId = "acct_test_456")
        assertThat(client.stripeAccountId).isEqualTo("acct_test_456")
    }
}
