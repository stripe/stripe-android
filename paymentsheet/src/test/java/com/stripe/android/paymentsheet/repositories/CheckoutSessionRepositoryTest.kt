package com.stripe.android.paymentsheet.repositories

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.checkouttesting.checkoutInit
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.hcaptcha.HCaptchaService
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
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
    private val hCaptchaService = mock<HCaptchaService>()

    private val repository = CheckoutSessionRepository(
        clientParams = clientParams,
        stripeNetworkClient = DefaultStripeNetworkClient(),
        analyticsRequestExecutor = analyticsRequestExecutor,
        paymentAnalyticsRequestFactory = PaymentAnalyticsRequestFactory(
            context = ApplicationProvider.getApplicationContext(),
            publishableKey = "pk_test_123",
        ),
        hCaptchaService = hCaptchaService,
        publishableKeyProvider = { "pk_test_123" },
        stripeAccountIdProvider = { null },
    )

    @Test
    fun `init sends elements_session_client params`() = runTest {
        val expectedSessionId = AnalyticsRequestFactory.sessionId.toString()
        networkRule.checkoutInit(
            bodyPart("elements_session_client[is_aggregation_expected]", "true"),
            bodyPart("elements_session_client[mobile_session_id]", expectedSessionId),
            bodyPart("elements_session_client[mobile_app_id]", clientParams.mobileAppId),
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
            bodyPart("elements_session_client[is_aggregation_expected]", "true"),
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

    @Test
    fun `updateCurrency fires currency_toggled on success`() = runTest {
        networkRule.checkoutUpdate { response ->
            response.testBodyFromFile("checkout-session-init.json")
        }

        repository.updateCurrency(
            sessionId = DEFAULT_CHECKOUT_SESSION_ID,
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
            currencyCode = "invalid",
        )

        assertThat(result.isFailure).isTrue()
        val params = analyticsRequestExecutor.getExecutedRequests().single().params
        assertThat(params).containsEntry("event", "elements.adaptive_pricing.currency_toggled.failed")
    }

    @Test
    fun `applyPromotionCode sends passive captcha token`() = runTest {
        whenever(hCaptchaService.passiveCaptchaToken(Int.MAX_VALUE))
            .thenReturn(HCaptchaService.Result.Success("captcha-token"))
        networkRule.checkoutUpdate(
            bodyPart("promotion_code", "SAVE10"),
            bodyPart("elements_session_client[is_aggregation_expected]", "true"),
            bodyPart("passive_captcha_token", "captcha-token"),
        ) { response ->
            response.testBodyFromFile("checkout-session-init.json")
        }

        val result = repository.applyPromotionCode(
            sessionId = DEFAULT_CHECKOUT_SESSION_ID,
            promotionCode = "SAVE10",
        )

        assertThat(result.isSuccess).isTrue()
        verify(hCaptchaService).passiveCaptchaToken(Int.MAX_VALUE)
    }
}
