package com.stripe.android

import com.google.common.truth.Truth.assertThat
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
class StripeClientTest {

    @Test
    fun `valid pk_test_ key is accepted`() {
        val client = StripeClient(publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        assertThat(client.publishableKey).isEqualTo(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
    }

    @Test
    fun `key starting with sk_ throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            StripeClient(publishableKey = "sk_test_123")
        }
    }

    @Test
    fun `blank key throws`() {
        assertFailsWith<IllegalArgumentException> {
            StripeClient(publishableKey = "")
        }
    }

    @Test
    fun `stripeAccountId is null by default`() {
        val client = StripeClient(publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        assertThat(client.stripeAccountId).isNull()
    }

    @Test
    fun `stripeAccountId is set correctly when provided`() {
        val client = StripeClient(
            publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            stripeAccountId = "acct_test_123",
        )
        assertThat(client.stripeAccountId).isEqualTo("acct_test_123")
    }
}
