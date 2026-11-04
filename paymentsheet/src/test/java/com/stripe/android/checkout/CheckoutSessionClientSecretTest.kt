package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.model.CheckoutSessionClientSecret
import org.junit.Assert.assertThrows
import org.junit.Test

class CheckoutSessionClientSecretTest {

    @Test
    fun `valid client secret exposes the session id`() {
        val clientSecret = CheckoutSessionClientSecret("cs_test_abc123_secret_xyz")

        assertThat(clientSecret.value).isEqualTo("cs_test_abc123_secret_xyz")
        assertThat(clientSecret.sessionId).isEqualTo("cs_test_abc123")
    }

    @Test
    fun `client secret without the delimiter is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            CheckoutSessionClientSecret("cs_test_abc123")
        }
    }

    @Test
    fun `client secret with an empty session id is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            CheckoutSessionClientSecret("_secret_abc")
        }
    }
}
