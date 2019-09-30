package com.stripe.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import org.json.JSONException
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GooglePayConfigTest {

    @Test
    @Throws(JSONException::class)
    fun getTokenizationSpecification_withoutConnectedAccount() {
        PaymentConfiguration.init(ApplicationProvider.getApplicationContext<Context>(),
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        val tokenizationSpec =
            GooglePayConfig(ApplicationProvider.getApplicationContext<Context>())
                .tokenizationSpecification
        val params = tokenizationSpec.getJSONObject("parameters")
        assertEquals("stripe",
            params.getString("gateway"))
        assertEquals(ApiVersion.get().code,
            params.getString("stripe:version"))
        assertEquals(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            params.getString("stripe:publishableKey"))
    }

    @Test
    @Throws(JSONException::class)
    fun getTokenizationSpecification_withConnectedAccount() {
        val tokenizationSpec = GooglePayConfig(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY, "acct_1234")
            .tokenizationSpecification
        val params = tokenizationSpec.getJSONObject("parameters")
        assertEquals("stripe",
            params.getString("gateway"))
        assertEquals(ApiVersion.get().code,
            params.getString("stripe:version"))
        assertEquals("pk_test_123/acct_1234",
            params.getString("stripe:publishableKey"))
    }
}
