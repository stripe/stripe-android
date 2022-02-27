package com.stripe.android.core

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.ApiVersion.Companion.API_VERSION_CODE
import org.junit.Test

class ApiVersionTest {
    @Test
    fun `instance should instantiate correctly`() {
        assertThat(ApiVersion.get().code).isEqualTo(API_VERSION_CODE)
    }

    @Test
    fun `single beta header should have correct code`() {
        val betaCode = "betaCode=v1"
        assertThat(ApiVersion(setOf(betaCode)).code)
            .isEqualTo("$API_VERSION_CODE;$betaCode")
    }
}
