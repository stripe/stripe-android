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

    @Test
    fun testApiKey_withTest_isNotLiveMode() {
        val options = ApiRequest.Options(apiKey = "pk_test_1234")

        assertThat(options.apiKeyIsLiveMode).isFalse()
    }

    @Test
    fun testApiKey_withLive_isLiveMode() {
        val options = ApiRequest.Options(apiKey = "pk_live_1234")

        assertThat(options.apiKeyIsLiveMode).isTrue()
    }

    @Test
    fun testApiKey_withoutLiveOrTest_isLiveMode() {
        val options = ApiRequest.Options(apiKey = "pk_other_1234")

        assertThat(options.apiKeyIsLiveMode).isTrue()
    }
}
