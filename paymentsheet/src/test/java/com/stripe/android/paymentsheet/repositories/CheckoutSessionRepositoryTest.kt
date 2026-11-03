package com.stripe.android.paymentsheet.repositories

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.checkouttesting.checkoutInit
import com.stripe.android.checkouttesting.checkoutPoll
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.DEFAULT_API_CONFIG
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.hasBodyPart
import com.stripe.android.networktesting.RequestMatchers.not
import com.stripe.android.networktesting.RequestMatchers.query
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
    fun `applyPromotionCode reports started and succeeded`() = runTest {
        networkRule.checkoutUpdate(
            bodyPart("promotion_code", "10OFF"),
            bodyPart("client_secret", "cs_test_abc123_secret_test"),
        ) { response ->
            response.testBodyFromFile("checkout-session-init.json")
        }

        repository.applyPromotionCode(
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(),
            promotionCode = "10OFF",
        ).getOrThrow()

        val started = analyticsRequestExecutor.awaitEvent("checkout_elements.apply_promotion_code.started")
        assertThat(started).containsEntry("checkout_session_id", DEFAULT_CHECKOUT_SESSION_ID)
        assertThat(started).doesNotContainKey("duration")
        val succeeded = analyticsRequestExecutor.awaitEvent("checkout_elements.apply_promotion_code.succeeded")
        assertThat(succeeded).containsEntry("checkout_session_id", DEFAULT_CHECKOUT_SESSION_ID)
        assertThat(succeeded).containsKey("duration")
        analyticsRequestExecutor.validate()
    }

    @Test
    fun `applyPromotionCode reports started and failed with the error`() = runTest {
        networkRule.checkoutUpdate { response ->
            response.setResponseCode(400)
            response.addHeader("Request-Id", "req_123")
            response.setBody(
                """
                    {
                        "error": {
                            "type": "invalid_request_error",
                            "code": "promotion_code_invalid",
                            "message": "Invalid promotion code"
                        }
                    }
                """.trimIndent()
            )
        }

        val result = repository.applyPromotionCode(
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(),
            promotionCode = "INVALID",
        )

        assertThat(result.isFailure).isTrue()
        analyticsRequestExecutor.awaitEvent("checkout_elements.apply_promotion_code.started")
        val failed = analyticsRequestExecutor.awaitEvent("checkout_elements.apply_promotion_code.failed")
        assertThat(failed).containsEntry("checkout_session_id", DEFAULT_CHECKOUT_SESSION_ID)
        assertThat(failed).containsEntry("error_type", "invalid_request_error")
        assertThat(failed).containsEntry("error_code", "promotion_code_invalid")
        assertThat(failed).containsEntry("request_id", "req_123")
        assertThat(failed).containsKey("duration")
        analyticsRequestExecutor.validate()
    }

    @Test
    fun `removePromotionCode sends an empty promotion code and reports started and succeeded`() = runTest {
        networkRule.checkoutUpdate(
            bodyPart("promotion_code", ""),
            bodyPart("client_secret", "cs_test_abc123_secret_test"),
        ) { response ->
            response.testBodyFromFile("checkout-session-init.json")
        }

        repository.removePromotionCode(
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(),
        ).getOrThrow()

        analyticsRequestExecutor.awaitEvent("checkout_elements.remove_promotion_code.started")
        val succeeded = analyticsRequestExecutor.awaitEvent("checkout_elements.remove_promotion_code.succeeded")
        assertThat(succeeded).containsKey("duration")
        analyticsRequestExecutor.validate()
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

        analyticsRequestExecutor.awaitEvent("elements.adaptive_pricing.currency_toggled")
        analyticsRequestExecutor.validate()
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
        analyticsRequestExecutor.awaitEvent("elements.adaptive_pricing.currency_toggled.failed")
        analyticsRequestExecutor.validate()
    }

    @Test
    fun `poll sends only the client secret and parses the response`() = runTest {
        networkRule.checkoutPoll(
            query("client_secret=cs_test_abc123_secret_test"),
        )

        val result = repository.poll(
            sessionId = DEFAULT_CHECKOUT_SESSION_ID,
            clientSecret = "cs_test_abc123_secret_test",
        )

        assertThat(result.getOrThrow()).isEqualTo(
            CheckoutClientPollResponse(
                id = DEFAULT_CHECKOUT_SESSION_ID,
                confirmationStatus = CheckoutClientPollResponse.ConfirmationStatus.SUCCEEDED,
                status = CheckoutSessionResponse.Status.COMPLETE,
                paymentStatus = CheckoutSessionResponse.PaymentStatus.PAID,
            )
        )
    }

    @Test
    fun `poll fails on unknown confirmation status`() = runTest {
        networkRule.checkoutPoll { response ->
            response.testBodyFromFile("checkout-session-poll.json") { json ->
                json.put("confirmation_status", "processing")
            }
        }

        val result = repository.poll(
            sessionId = DEFAULT_CHECKOUT_SESSION_ID,
            clientSecret = "cs_test_abc123_secret_test",
        )

        assertThat(result.exceptionOrNull()).isInstanceOf(APIException::class.java)
    }
}
