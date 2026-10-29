package com.stripe.android.core.networking

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.ApiKeyFixtures
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RequestBetasTest {
    @Test
    fun `configured flags survive explicit alternate credentials and idempotency`() {
        val configuration = ApiConfiguration(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)
            .stripeAccountId("acct_merchant")
            .betas(setOf("example=v1"))
            .build()
        val credentials = ApiRequest.Options("ek_test_123", stripeAccount = null, idempotencyKey = "operation-key")
        val request = ApiRequest.Factory().createPost(
            url = ApiRequest.API_HOST,
            options = credentials,
            params = null,
            apiConfiguration = configuration,
        )

        assertThat(request.headers["Authorization"]).isEqualTo("Bearer ek_test_123")
        assertThat(request.headers).doesNotContainKey("Stripe-Account")
        assertThat(request.headers["Idempotency-Key"]).isEqualTo("operation-key")
        assertThat(request.headers["Stripe-Version"]).isEqualTo("2020-03-02;example=v1")
    }

    @Test
    fun `configured flags preserve pinned version and built in tokens`() {
        val configuration = ApiConfiguration(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)
            .betas(setOf("z=v1", "built_in=v1", "a=v2"))
            .build()
        val request = ApiRequest.Factory(apiVersion = "2025-01-01;built_in=v1").createGet(
            url = ApiRequest.API_HOST,
            options = ApiRequest.Options(configuration.publishableKey, stripeAccount = null),
            params = null,
            apiConfiguration = configuration,
        )

        assertThat(request.headers["Stripe-Version"]).isEqualTo("2025-01-01;built_in=v1;a=v2;z=v1")
    }

    @Test
    fun `request headers retain their snapshot across separate configurations`() {
        val builder = ApiConfiguration(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY).betas(setOf("first=v1"))
        val factory = ApiRequest.Factory()
        val credentials = ApiRequest.Options(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY, stripeAccount = null)
        val first = factory.createGet(ApiRequest.API_HOST, credentials, null, builder.build())
        builder.betas(setOf("second=v1"))
        val second = factory.createGet(ApiRequest.API_HOST, credentials, null, builder.build())
        val legacy = factory.createGet(ApiRequest.API_HOST, credentials)

        assertThat(first.headers["Stripe-Version"]).isEqualTo("2020-03-02;first=v1")
        assertThat(second.headers["Stripe-Version"]).isEqualTo("2020-03-02;second=v1")
        assertThat(legacy.headers["Stripe-Version"]).isEqualTo("2020-03-02")
    }
}
