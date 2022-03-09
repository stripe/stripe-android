package com.stripe.android.core.networking

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.ApiKeyFixtures
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ApiRequestOptionsTest {

    @Test
    fun testCreate() {
        val opts = ApiRequest.Options(
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            "account"
        )
        assertThat(opts.apiKey).isEqualTo(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        assertThat(opts.stripeAccount).isEqualTo("account")
    }

    @Test
    fun testCreate_withSecretKey_throwsException() {
        assertFailsWith<IllegalArgumentException> {
            ApiRequest.Options(ApiKeyFixtures.FAKE_SECRET_KEY)
        }
    }
}
