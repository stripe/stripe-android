package com.stripe.android.cards

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.networking.AnalyticsRequest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class DefaultCardAccountRangeRepositoryFactoryTest {
    private val analyticsRequests = mutableListOf<AnalyticsRequest>()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val factory = DefaultCardAccountRangeRepositoryFactory(
        context = context,
        productUsageTokens = setOf("SomeProduct"),
    ) {
        analyticsRequests.add(it)
    }

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

        val event = analyticsRequests.first()

        assertThat(event.params["event"]).isEqualTo("stripe_android.card_metadata_pk_unavailable")
        assertThat(event.params["product_usage"]).isEqualTo("SomeProduct")
    }

    @Test
    fun `create() with config should succeed`() {
        PaymentConfiguration.init(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        assertThat(factory.create())
            .isNotNull()
        assertThat(analyticsRequests)
            .hasSize(1)

        val event = analyticsRequests.first()

        assertThat(event.params["event"]).isEqualTo("stripe_android.card_metadata_pk_available")
        assertThat(event.params["product_usage"]).isEqualTo("SomeProduct")
    }
}
