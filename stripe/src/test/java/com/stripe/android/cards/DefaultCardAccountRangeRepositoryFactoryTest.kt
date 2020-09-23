package com.stripe.android.cards

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.AnalyticsRequest
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class DefaultCardAccountRangeRepositoryFactoryTest {
    private val analyticsRequests = mutableListOf<AnalyticsRequest>()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val factory = DefaultCardAccountRangeRepositoryFactory(
        context,
        { analyticsRequests.add(it) },
        AnalyticsRequest.Factory()
    )

    @BeforeTest
    fun setup() {
        PaymentConfiguration.clearInstance()
    }

    @Test
    fun `create() without config should succeed`() {
        assertThat(factory.create())
            .isNotNull()
        assertThat(analyticsRequests)
            .hasSize(1)
        assertThat(analyticsRequests.first().params["event"])
            .isEqualTo("stripe_android.card_metadata_pk_unavailable")
    }

    @Test
    fun `create() with config should succeed`() {
        PaymentConfiguration.init(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        assertThat(factory.create())
            .isNotNull()
        assertThat(analyticsRequests)
            .hasSize(1)
        assertThat(analyticsRequests.first().params["event"])
            .isEqualTo("stripe_android.card_metadata_pk_available")
    }
}
