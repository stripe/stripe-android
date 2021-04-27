package com.stripe.android

import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiVersion.Companion.API_VERSION_CODE
import org.junit.Test

class ApiVersionTest {
    @Test
    fun `instance should instantiate correctly`() {
        assertThat(ApiVersion.get().code).isEqualTo(API_VERSION_CODE)
    }

    @Test
    fun `wechat pay beta should have correct code`() {
        assertThat(ApiVersion.get().withBetas(setOf(StripeApiBeta.WechatPayV1)).code)
            .isEqualTo("$API_VERSION_CODE;${StripeApiBeta.WechatPayV1.code}")
    }
}
