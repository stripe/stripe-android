package com.stripe.android.core.networking

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.ApiKeyFixtures
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ApiRequestOptionsTest {

    @Test
    fun testCreate() {
        val opts = ApiConfiguration.State(
            publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            stripeAccountId = "account",
        ).toRequestOptions(idempotencyKey = null)
        assertThat(opts.apiKey).isEqualTo(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        assertThat(opts.stripeAccount).isEqualTo("account")
    }

    @Test
    fun `request options retain the idempotency key`() {
        val configuration = ApiConfiguration.State(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY, "account")

        val options = configuration.toRequestOptions(idempotencyKey = "request-id")

        assertThat(options.idempotencyKey).isEqualTo("request-id")
    }

    @Test
    fun `alternate credentials preserve idempotency without changing configuration`() {
        val configuration = ApiConfiguration.State(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY, "account")

        val options = configuration.withRequestCredentials(
            apiKey = "ek_test_customer",
            stripeAccount = "customer-account",
            idempotencyKey = "request-id",
        )

        assertThat(options.apiKey).isEqualTo("ek_test_customer")
        assertThat(options.stripeAccount).isEqualTo("customer-account")
        assertThat(options.idempotencyKey).isEqualTo("request-id")
        assertThat(configuration.publishableKey).isEqualTo(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        assertThat(configuration.stripeAccountId).isEqualTo("account")
    }

    @Test
    fun `explicit null credentials override clears the account`() {
        val configuration = ApiConfiguration.State(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY, "account")

        val options = configuration.withRequestCredentials(
            apiKey = "pk_test_alternate",
            stripeAccount = null,
            idempotencyKey = null,
        )

        assertThat(options.apiKey).isEqualTo("pk_test_alternate")
        assertThat(options.stripeAccount).isNull()
    }

    @Test
    fun `changing the account preserves credentials and idempotency`() {
        val original = ApiConfiguration.State(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY, "account")
            .toRequestOptions(idempotencyKey = "request-id")

        val options = original.withStripeAccount(stripeAccount = null)

        assertThat(options.apiKey).isEqualTo(original.apiKey)
        assertThat(options.stripeAccount).isNull()
        assertThat(options.idempotencyKey).isEqualTo("request-id")
        assertThat(original.stripeAccount).isEqualTo("account")
    }

    @Test
    fun `changing the key preserves account and idempotency`() {
        val original = ApiConfiguration.State(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY, "account")
            .toRequestOptions(idempotencyKey = "request-id")

        val options = original.withApiKey(apiKey = "ek_test_customer")

        assertThat(options.apiKey).isEqualTo("ek_test_customer")
        assertThat(options.stripeAccount).isEqualTo("account")
        assertThat(options.idempotencyKey).isEqualTo("request-id")
        assertThat(original.apiKey).isEqualTo(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
    }

    @Test
    fun `changing the key rejects secret keys`() {
        val options = ApiConfiguration.State(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY, null)
            .toRequestOptions(idempotencyKey = null)

        assertFailsWith<IllegalArgumentException> {
            options.withApiKey(apiKey = ApiKeyFixtures.FAKE_SECRET_KEY)
        }
    }

    @Test
    fun testCreate_withSecretKey_throwsException() {
        assertFailsWith<IllegalArgumentException> {
            ApiConfiguration.State(ApiKeyFixtures.FAKE_SECRET_KEY, null).toRequestOptions(idempotencyKey = null)
        }
    }

    @Test
    fun testApiKey_withTest_isNotLiveMode() {
        val options = ApiConfiguration.State("pk_test_1234", null).toRequestOptions(idempotencyKey = null)

        assertThat(options.apiKeyIsLiveMode).isFalse()
    }

    @Test
    fun testApiKey_withLive_isLiveMode() {
        val options = ApiConfiguration.State("pk_live_1234", null).toRequestOptions(idempotencyKey = null)

        assertThat(options.apiKeyIsLiveMode).isTrue()
    }

    @Test
    fun testApiKey_withoutLiveOrTest_isLiveMode() {
        val options = ApiConfiguration.State("pk_other_1234", null).toRequestOptions(idempotencyKey = null)

        assertThat(options.apiKeyIsLiveMode).isTrue()
    }
}
