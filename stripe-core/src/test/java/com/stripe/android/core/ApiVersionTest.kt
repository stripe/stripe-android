package com.stripe.android.core

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.ApiVersion.Companion.API_VERSION_CODE
import org.junit.Test

class ApiVersionTest {
    @Test
    fun `empty configured tokens preserve the producer version exactly`() {
        val version = "2025-01-01;built_in=v1;other=v2"
        assertThat(ApiVersion.withBetas(version, emptySet())).isEqualTo(version)
    }

    @Test
    fun `configured tokens are sorted and deduplicated against built in tokens`() {
        assertThat(
            ApiVersion.withBetas("2025-01-01;built_in=v1", setOf("z=v1", "built_in=v1", "a=v2"))
        ).isEqualTo("2025-01-01;built_in=v1;a=v2;z=v1")
    }

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
