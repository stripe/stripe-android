package com.stripe.android.payments

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.networking.AnalyticsDataFactory
import com.stripe.android.networking.AnalyticsRequest
import com.stripe.android.networking.AnalyticsRequestExecutor
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class StripeBrowserLauncherViewModelTest {
    private val analyticsRequests = mutableListOf<AnalyticsRequest>()
    private val analyticsRequestExecutor = AnalyticsRequestExecutor {
        analyticsRequests.add(it)
    }
    private val analyticsRequestFactory = AnalyticsRequest.Factory()

    private val analyticsDataFactory = AnalyticsDataFactory(
        ApplicationProvider.getApplicationContext(),
        ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
    )

    private val viewModel = StripeBrowserLauncherViewModel(
        analyticsRequestExecutor,
        analyticsRequestFactory,
        analyticsDataFactory
    )

    @Test
    fun `logCapabilities() when shouldUseCustomTabs = true should log expected event`() {
        viewModel.logCapabilities(shouldUseCustomTabs = true)

        assertThat(analyticsRequests)
            .hasSize(1)

        assertThat(analyticsRequests.first().params["event"])
            .isEqualTo("stripe_android.auth_with_customtabs")
    }

    @Test
    fun `logCapabilities() when shouldUseCustomTabs = false should log expected event`() {
        viewModel.logCapabilities(shouldUseCustomTabs = false)

        assertThat(analyticsRequests)
            .hasSize(1)

        assertThat(analyticsRequests.first().params["event"])
            .isEqualTo("stripe_android.auth_with_defaultbrowser")
    }
}
