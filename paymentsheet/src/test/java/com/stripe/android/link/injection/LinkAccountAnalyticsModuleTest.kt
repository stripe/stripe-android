package com.stripe.android.link.injection

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.TestFactory
import com.stripe.android.link.analytics.LinkEvent
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LinkAccountAnalyticsModuleTest {
    @Test
    fun `request factory uses Link configuration publishable key`() {
        val factory = LinkAccountAnalyticsModule.providePaymentAnalyticsRequestFactory(
            context = ApplicationProvider.getApplicationContext<Context>(),
            configuration = TestFactory.LINK_CONFIGURATION,
            productUsageTokens = emptySet(),
        )

        val request = factory.createRequest(
            event = LinkEvent.AccountLookupComplete,
            additionalParams = emptyMap(),
        )

        assertThat(request.params["publishable_key"])
            .isEqualTo(TestFactory.LINK_CONFIGURATION.apiConfiguration.publishableKey)
    }
}
