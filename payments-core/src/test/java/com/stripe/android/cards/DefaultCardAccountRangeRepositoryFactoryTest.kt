package com.stripe.android.cards

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.networking.StripeRepository
import com.stripe.android.networktesting.TestApiKeys
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class DefaultCardAccountRangeRepositoryFactoryTest {
    private val analyticsRequests = mutableListOf<AnalyticsRequest>()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `create() without config should succeed`() {
        val factory = getFactory { throw IllegalStateException() }
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
        val factory = getFactory {
            ApiConfiguration.State(
                TestApiKeys.PUBLISHABLE,
                TestApiKeys.ACCOUNT
            )
        }
        assertThat(factory.create())
            .isNotNull()
        assertThat(analyticsRequests)
            .hasSize(1)

        val event = analyticsRequests.first()

        assertThat(event.params["event"]).isEqualTo("stripe_android.card_metadata_pk_available")
        assertThat(event.params["publishable_key"]).isEqualTo(TestApiKeys.PUBLISHABLE)
        assertThat(event.params["product_usage"]).isEqualTo("SomeProduct")
    }

    private fun getFactory(apiConfigProvider: () -> ApiConfiguration.State): DefaultCardAccountRangeRepositoryFactory {
        return DefaultCardAccountRangeRepositoryFactory(
            context = context,
            productUsageTokens = setOf("SomeProduct"),
            requestSurface = StripeRepository.DEFAULT_REQUEST_SURFACE,
            analyticsRequestExecutor = { analyticsRequests.add(it) },
            apiConfigurationProvider = apiConfigProvider
        )
    }
}
