package com.stripe.android.paymentsheet.repositories

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.checkouttesting.checkoutInit
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.DEFAULT_API_CONFIG
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.hasBodyPart
import com.stripe.android.networktesting.RequestMatchers.not
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
import com.stripe.android.utils.FakeFraudDetectionDataRepository
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CheckoutSessionRepositoryTest {

    @get:Rule
    val networkRule = NetworkRule()

    private val clientParams = ElementsSessionClientParams(
        mobileAppId = "com.stripe.android.paymentsheet.test",
        mobileSessionIdProvider = { AnalyticsRequestFactory.sessionId.toString() },
    )

    private val analyticsRequestExecutor = FakeAnalyticsRequestExecutor()

    private val repository = CheckoutSessionRepository(
        stripeRepository = CheckoutStripeRepositoryFactory.create(),
        elementsSessionClientParams = CheckoutStripeRepositoryFactory.clientParams(),
        fraudDetectionDataRepository = FakeFraudDetectionDataRepository.create(),
        stripeNetworkClient = DefaultStripeNetworkClient(),
        analyticsRequestExecutor = analyticsRequestExecutor,
        paymentAnalyticsRequestFactory = PaymentAnalyticsRequestFactory(
            context = ApplicationProvider.getApplicationContext(),
            publishableKey = "pk_test_123",
        ),
        apiRequestOptionsProvider = {
            ApiRequest.Options(
                apiKey = DEFAULT_API_CONFIG.publishableKey,
                stripeAccount = DEFAULT_API_CONFIG.stripeAccountId,
            )
        },
    )

    @Test
    fun `init sends CheckoutClient common params`() = runTest {
        val expectedSessionId = AnalyticsRequestFactory.sessionId.toString()
        networkRule.checkoutInit(
            bodyPart("client_secret", "cs_test_abc123_secret_test"),
            bodyPart("adaptive_pricing[allowed]", "true"),
            bodyPart("locale", clientParams.locale),
            bodyPart("timezone", java.util.TimeZone.getDefault().id),
            bodyPart("mobile_session_id", expectedSessionId),
            bodyPart("mobile_app_id", clientParams.mobileAppId),
        ) { response ->
            response.testBodyFromFile("checkout-session-init.json")
        }

        val result = repository.init(
            sessionId = DEFAULT_CHECKOUT_SESSION_ID,
            clientSecret = "cs_test_abc123_secret_test",
            adaptivePricingAllowed = true,
        )

        assertThat(result.getOrThrow().clientSecret).isEqualTo("cs_test_abc123_secret_test")
    }

    @Test
    fun `detach sends CheckoutClient common params`() = runTest {
        val expectedSessionId = AnalyticsRequestFactory.sessionId.toString()
        networkRule.checkoutUpdate(
            bodyPart("payment_method_to_detach", "pm_123"),
            not(hasBodyPart("timezone")),
            bodyPart("client_secret", "cs_test_abc123_secret_test"),
            bodyPart("locale", clientParams.locale),
            bodyPart("mobile_session_id", expectedSessionId),
            bodyPart("mobile_app_id", clientParams.mobileAppId),
        ) { response ->
            response.testBodyFromFile("checkout-session-init.json")
        }

        val result = repository.detachPaymentMethod(
            sessionId = DEFAULT_CHECKOUT_SESSION_ID,
            clientSecret = "cs_test_abc123_secret_test",
            paymentMethodId = "pm_123",
        )

        assertThat(result.getOrThrow().clientSecret).isEqualTo("cs_test_abc123_secret_test")
    }

    @Test
    fun `updateCurrency sends currency code and returns response on success`() = runTest {
        networkRule.checkoutUpdate(
            bodyPart("adaptive_pricing[currency]", "eur"),
            bodyPart("client_secret", "cs_test_abc123_secret_test"),
        ) { response ->
            response.testBodyFromFile("checkout-session-init.json")
        }

        val result = repository.updateCurrency(
            sessionId = DEFAULT_CHECKOUT_SESSION_ID,
            clientSecret = "cs_test_abc123_secret_test",
            currencyCode = "eur",
        )

        assertThat(result.getOrThrow().clientSecret).isEqualTo("cs_test_abc123_secret_test")
    }

    @Test
    fun `updateCurrency returns failure on error response`() = runTest {
        networkRule.checkoutUpdate { response ->
            response.setResponseCode(400)
            response.setBody("""{"error": {"message": "Invalid currency"}}""")
        }

        val result = repository.updateCurrency(
            sessionId = DEFAULT_CHECKOUT_SESSION_ID,
            clientSecret = "cs_test_abc123_secret_test",
            currencyCode = "invalid",
        )

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `updateCurrency fires currency_toggled on success`() = runTest {
        networkRule.checkoutUpdate { response ->
            response.testBodyFromFile("checkout-session-init.json")
        }

        repository.updateCurrency(
            sessionId = DEFAULT_CHECKOUT_SESSION_ID,
            clientSecret = "cs_test_abc123_secret_test",
            currencyCode = "eur",
        ).getOrThrow()

        val params = analyticsRequestExecutor.getExecutedRequests().single().params
        assertThat(params).containsEntry("event", "elements.adaptive_pricing.currency_toggled")
    }

    @Test
    fun `updateCurrency fires currency_toggled_failed on failure`() = runTest {
        networkRule.checkoutUpdate { response ->
            response.setResponseCode(400)
            response.setBody("""{"error": {"message": "Invalid currency"}}""")
        }

        val result = repository.updateCurrency(
            sessionId = DEFAULT_CHECKOUT_SESSION_ID,
            clientSecret = "cs_test_abc123_secret_test",
            currencyCode = "invalid",
        )

        assertThat(result.isFailure).isTrue()
        val params = analyticsRequestExecutor.getExecutedRequests().single().params
        assertThat(params).containsEntry("event", "elements.adaptive_pricing.currency_toggled.failed")
    }
}
