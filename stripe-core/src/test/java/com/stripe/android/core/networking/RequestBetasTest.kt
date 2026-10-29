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
    fun `request flags survive credential override without inheriting the merchant account`() {
        val state = ApiConfiguration(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)
            .stripeAccountId("acct_merchant")
            .betas(setOf("example=v1"))
            .build()
        val options = state.withRequestCredentials("ek_test_123", null, "operation-key")
        val headers = RequestHeadersFactory.Api(options).create()

        assertThat(headers["Authorization"]).isEqualTo("Bearer ek_test_123")
        assertThat(headers).doesNotContainKey("Stripe-Account")
        assertThat(headers["Idempotency-Key"]).isEqualTo("operation-key")
        assertThat(headers["Stripe-Version"]).isEqualTo("2020-03-02;example=v1")
    }

    @Test
    fun `normal and multipart headers preserve built in version tokens`() {
        val options = ApiConfiguration(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)
            .betas(setOf("z=v1", "built_in=v1", "a=v2"))
            .build().toRequestOptions(idempotencyKey = null)
        val normal = RequestHeadersFactory.Api(options, apiVersion = "2025-01-01;built_in=v1").create()
        val upload = RequestHeadersFactory.FileUpload(
            options = options,
            apiVersion = "2025-01-01;built_in=v1",
            boundary = "test-boundary",
        ).create()

        assertThat(normal["Stripe-Version"]).isEqualTo("2025-01-01;built_in=v1;a=v2;z=v1")
        assertThat(upload["Stripe-Version"]).isEqualTo(normal["Stripe-Version"])
    }

    @Test
    fun `request headers retain their snapshot after configuration changes`() {
        val builder = ApiConfiguration(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY).betas(setOf("first=v1"))
        val options = builder.build().toRequestOptions(idempotencyKey = null)
        val request = ApiRequest.Factory().createGet(ApiRequest.API_HOST, options)
        builder.betas(setOf("second=v1"))

        assertThat(request.headers["Stripe-Version"]).isEqualTo("2020-03-02;first=v1")
        assertThat(options.copy(apiKey = "ek_test_123", stripeAccount = null).betas).containsExactly("first=v1")
    }
}
