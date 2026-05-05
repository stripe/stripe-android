package com.stripe.android.paymentsheet.repositories

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.checkouttesting.checkoutInit
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.core.utils.urlEncode
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.testBodyFromFile
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CheckoutSessionRepositoryTest {

    @get:Rule
    val networkRule = NetworkRule()

    private val repository = CheckoutSessionRepository(
        stripeNetworkClient = DefaultStripeNetworkClient(),
        publishableKeyProvider = { "pk_test_123" },
        stripeAccountIdProvider = { null },
        context = ApplicationProvider.getApplicationContext(),
    )

    @Test
    fun `init sends elements_session_client params`() = runTest {
        val expectedSessionId = AnalyticsRequestFactory.sessionId.toString()
        val expectedAppId = ApplicationProvider
            .getApplicationContext<android.app.Application>().packageName
        networkRule.checkoutInit(
            bodyPart(urlEncode("elements_session_client[is_aggregation_expected]"), "true"),
            bodyPart(urlEncode("elements_session_client[mobile_session_id]"), expectedSessionId),
            bodyPart(urlEncode("elements_session_client[mobile_app_id]"), expectedAppId),
        ) { response ->
            response.testBodyFromFile("checkout-session-init.json")
        }

        val result = repository.init(
            sessionId = DEFAULT_CHECKOUT_SESSION_ID,
            adaptivePricingAllowed = true,
        )

        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `updateCurrency sends currency code and returns response on success`() = runTest {
        networkRule.checkoutUpdate(
            bodyPart("updated_currency", "eur"),
            bodyPart(urlEncode("elements_session_client[is_aggregation_expected]"), "true"),
        ) { response ->
            response.testBodyFromFile("checkout-session-init.json")
        }

        val result = repository.updateCurrency(
            sessionId = DEFAULT_CHECKOUT_SESSION_ID,
            currencyCode = "eur",
        )

        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `updateCurrency returns failure on error response`() = runTest {
        networkRule.checkoutUpdate { response ->
            response.setResponseCode(400)
            response.setBody("""{"error": {"message": "Invalid currency"}}""")
        }

        val result = repository.updateCurrency(
            sessionId = DEFAULT_CHECKOUT_SESSION_ID,
            currencyCode = "invalid",
        )

        assertThat(result.isFailure).isTrue()
    }
}
