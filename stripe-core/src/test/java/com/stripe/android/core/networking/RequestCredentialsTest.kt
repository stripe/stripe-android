package com.stripe.android.core.networking

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.ApiKeyFixtures
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RequestCredentialsTest {

    @Test
    fun `absent override inherits session credentials`() {
        val configuration = ApiConfiguration.State("pk_test_123", "acct_merchant", setOf("example=v1"))
        val credentials = resolveRequestCredentials(configuration, null)

        assertThat(credentials.apiKey).isEqualTo(configuration.publishableKey)
        assertThat(credentials.stripeAccount).isEqualTo(configuration.stripeAccountId)
    }

    @Test
    fun `explicit override keeps a null account and masked diagnostics`() {
        val configuration = ApiConfiguration.State("pk_test_123", "acct_merchant", emptySet())
        val override = RequestCredentials("ek_test_123", null)
        val credentials = resolveRequestCredentials(configuration, override)

        assertThat(credentials.apiKey).isEqualTo("ek_test_123")
        assertThat(credentials.stripeAccount).isNull()
        assertThat(credentials.toString()).doesNotContain(credentials.apiKey)
    }

    @Test
    fun testCreate() {
        val opts = RequestCredentials(
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            "account"
        )
        assertThat(opts.apiKey).isEqualTo(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        assertThat(opts.stripeAccount).isEqualTo("account")
    }

    @Test
    fun testCreate_withSecretKey_throwsException() {
        assertFailsWith<IllegalArgumentException> {
            RequestCredentials(ApiKeyFixtures.FAKE_SECRET_KEY, stripeAccount = null)
        }
    }

    @Test
    fun testApiKey_withTest_isNotLiveMode() {
        val options = RequestCredentials(apiKey = "pk_test_1234", stripeAccount = null)

        assertThat(options.apiKeyIsLiveMode).isFalse()
    }

    @Test
    fun testApiKey_withLive_isLiveMode() {
        val options = RequestCredentials(apiKey = "pk_live_1234", stripeAccount = null)

        assertThat(options.apiKeyIsLiveMode).isTrue()
    }

    @Test
    fun testApiKey_withoutLiveOrTest_isLiveMode() {
        val options = RequestCredentials(apiKey = "pk_other_1234", stripeAccount = null)

        assertThat(options.apiKeyIsLiveMode).isTrue()
    }
}
