package com.stripe.android.core

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.ApiVersion.Companion.API_VERSION_CODE
import org.junit.Test

class ApiVersionTest {
    @Test
    fun `instance should instantiate correctly`() {
        assertThat(ApiVersion.get().code).isEqualTo("2026-08-26.dahlia")
    }

    @Test
    fun `multiple beta headers preserve their suffixes`() {
        assertThat(
            ApiVersion(setOf("financial_connections_client_api_beta=v1", "another_beta=v2")).code
        ).isEqualTo("2026-08-26.dahlia;financial_connections_client_api_beta=v1;another_beta=v2")
    }

    @Test
    fun `single beta header should have correct code`() {
        val betaCode = "betaCode=v1"
        assertThat(ApiVersion(setOf(betaCode)).code)
            .isEqualTo("$API_VERSION_CODE;$betaCode")
    }
}
