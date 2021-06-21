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
    fun `single beta header should have correct code`() {
        assertThat(ApiVersion(setOf(StripeApiBeta.WeChatPayV1)).code)
            .isEqualTo("$API_VERSION_CODE;${StripeApiBeta.WeChatPayV1.code}")
    }
}
