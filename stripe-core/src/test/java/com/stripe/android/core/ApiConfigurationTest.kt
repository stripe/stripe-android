package com.stripe.android.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ApiConfigurationTest {
    @Test
    fun `beta tokens are empty by default`() {
        val state = ApiConfiguration.State(publishableKey = "pk_test_123", stripeAccountId = null)

        assertThat(state.betas).isEmpty()
        assertThat(ApiConfiguration("pk_test_123").build().betas).isEmpty()
    }

    @Test
    fun `configured tokens are isolated from input mutation and builder reuse`() {
        val tokens = mutableSetOf("example_beta=v1")
        val builder = ApiConfiguration("pk_test_123").betas(tokens)
        tokens.add("other_beta=v2")
        val first = builder.build()
        builder.betas(setOf("replacement=v1"))

        assertThat(first.betas).containsExactly("example_beta=v1")
        assertThat(builder.build().betas).containsExactly("replacement=v1")
    }
}
