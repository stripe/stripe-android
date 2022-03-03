package com.stripe.android

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.ApiVersion
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class GooglePayConfigTest {

    @Test
    fun getTokenizationSpecification_withoutConnectedAccount() {
        PaymentConfiguration.init(
            ApplicationProvider.getApplicationContext(),
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
        )
        val tokenizationSpec =
            GooglePayConfig(ApplicationProvider.getApplicationContext())
                .tokenizationSpecification
        val params = tokenizationSpec.getJSONObject("parameters")
        assertThat(params.getString("gateway"))
            .isEqualTo("stripe")
        assertThat(params.getString("stripe:version"))
            .isEqualTo(ApiVersion.get().code)
        assertThat(params.getString("stripe:publishableKey"))
            .isEqualTo(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
    }

    @Test
    fun getTokenizationSpecification_withConnectedAccount() {
        val tokenizationSpec = GooglePayConfig(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY, "acct_1234")
            .tokenizationSpecification
        val params = tokenizationSpec.getJSONObject("parameters")
        assertThat(params.getString("gateway"))
            .isEqualTo("stripe")
        assertThat(params.getString("stripe:version"))
            .isEqualTo(ApiVersion.get().code)
        assertThat(params.getString("stripe:publishableKey"))
            .isEqualTo("pk_test_123/acct_1234")
    }
}
