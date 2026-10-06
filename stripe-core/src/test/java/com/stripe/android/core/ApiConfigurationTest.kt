package com.stripe.android.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ApiConfigurationTest {
    @Test
    fun `betas are empty by default`() {
        val state = ApiConfiguration("pk_test_123").build()

        assertThat(state.betas).isEmpty()
    }

    @Test
    fun `betas are included in built state`() {
        val state = ApiConfiguration("pk_test_123")
            .betas(setOf("alipay_beta=v1"))
            .build()

        assertThat(state.betas).containsExactly("alipay_beta=v1")
    }

    @Test
    fun `built state snapshots mutable beta set`() {
        val betas = mutableSetOf("alipay_beta=v1")
        val state = ApiConfiguration("pk_test_123").betas(betas).build()

        betas.add("other_beta=v1")

        assertThat(state.betas).containsExactly("alipay_beta=v1")
    }
}
