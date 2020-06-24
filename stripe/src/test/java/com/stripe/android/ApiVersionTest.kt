package com.stripe.android

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ApiVersionTest {
    @Test
    fun `instance should instantiate correctly`() {
        assertThat(ApiVersion.get().code).isEqualTo("2020-03-02")
    }

    @Test
    fun `alipay beta should have correct code`() {
        assertThat(ApiVersion.get().withBetas(setOf(StripeApiBeta.AlipayV1)).code)
            .isEqualTo("2020-03-02;alipay_beta=v1")
    }

    @Test
    fun `oxxo beta should have correct code`() {
        assertThat(ApiVersion.get().withBetas(setOf(StripeApiBeta.OxxoV1)).code)
            .isEqualTo("2020-03-02;oxxo_beta=v1")
    }

    @Test
    fun `betas should combine correctly`() {
        assertThat(ApiVersion.get().withBetas(setOf(StripeApiBeta.AlipayV1, StripeApiBeta.OxxoV1)).code)
            .isEqualTo("2020-03-02;alipay_beta=v1;oxxo_beta=v1")
    }
}
