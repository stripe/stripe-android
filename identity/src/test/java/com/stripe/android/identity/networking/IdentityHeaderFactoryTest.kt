package com.stripe.android.identity.networking

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.HEADER_AUTHORIZATION
import com.stripe.android.core.networking.HEADER_STRIPE_VERSION
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class IdentityHeaderFactoryTest {
    @Test
    fun `headers use Dahlia with the Identity beta and ephemeral key`() {
        val headers = IdentityHeaderFactory.createHeaderWithEphemeralKey("ek_test_12345")

        assertThat(headers[HEADER_STRIPE_VERSION]).isEqualTo("2026-08-26.dahlia;identity_client_api=v8")
        assertThat(headers[HEADER_AUTHORIZATION]).isEqualTo("Bearer ek_test_12345")
    }
}
