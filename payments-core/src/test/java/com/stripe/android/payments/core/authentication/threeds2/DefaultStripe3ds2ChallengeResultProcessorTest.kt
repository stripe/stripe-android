package com.stripe.android.payments.core.authentication.threeds2

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.StripeIntentResult
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.RetryDelaySupplier
import com.stripe.android.model.Stripe3ds2AuthResult
import com.stripe.android.model.Stripe3ds2AuthResultFixtures
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.stripe3ds2.transaction.ChallengeResult
import com.stripe.android.stripe3ds2.transaction.IntentData
import com.stripe.android.stripe3ds2.transactions.UiType
import com.stripe.android.testing.AbsFakeStripeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class DefaultStripe3ds2ChallengeResultProcessorTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private val application = ApplicationProvider.getApplicationContext<Application>()

    private val analyticsRequests = mutableListOf<AnalyticsRequest>()
    private val analyticsRequestExecutor = AnalyticsRequestExecutor {
        analyticsRequests.add(it)
    }
    private val analyticsRequestFactory = PaymentAnalyticsRequestFactory(
        application,
        ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
    )

    private val stripeRepository = FakeStripeRepository()
    private val resultProcessor = DefaultStripe3ds2ChallengeResultProcessor(
        stripeRepository,
        analyticsRequestExecutor,
        analyticsRequestFactory,
        RetryDelaySupplier(),
        Logger.noop(),
        testDispatcher
    )

    @BeforeTest
    fun before() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun `process() when completion endpoint call succeeds should return expected flowOutcome`() =
        runTest {
            val paymentFlowResult = resultProcessor.process(SUCCEEDED)

            assertThat(stripeRepository.sourceIds)
                .containsExactly("src_id")

            assertThat(paymentFlowResult)
                .isEqualTo(
                    PaymentFlowResult.Unvalidated(
                        clientSecret = "client_secret",
                        flowOutcome = StripeIntentResult.Outcome.SUCCEEDED,
                        stripeAccountId = "acct_123"
                    )
                )
        }

    @Test
    fun `process() when completion endpoint call fails should return expected flowOutcome`() =
        runTest {
            stripeRepository.completionValue = {
                Result.failure(IllegalStateException("Failed"))
            }

            val paymentFlowResult = resultProcessor.process(SUCCEEDED)

            assertThat(stripeRepository.sourceIds)
                .containsExactly("src_id")

            assertThat(paymentFlowResult)
                .isEqualTo(
                    PaymentFlowResult.Unvalidated(
                        clientSecret = "client_secret",
                        flowOutcome = StripeIntentResult.Outcome.FAILED,
                        stripeAccountId = "acct_123"
                    )
                )
        }

    @Test
    fun `complete3ds2Auth() should retry until max retries are attempted due to a 4xx response`() =
        runTest {
            stripeRepository.completionValue = {
                Result.failure(InvalidRequestException(statusCode = 400))
            }

            resultProcessor.process(SUCCEEDED)

            assertThat(stripeRepository.complete3ds2AuthInvocations)
                .isEqualTo(4)
        }

    @Test
    fun `complete3ds2Auth() should succeed after a single retry failure due to a 4xx response`() =
        runTest {
            stripeRepository.completionValue = {
                if (stripeRepository.complete3ds2AuthInvocations <= 2) {
                    Result.failure(InvalidRequestException(statusCode = 400))
                } else {
                    Result.success(Stripe3ds2AuthResultFixtures.CHALLENGE_COMPLETION)
                }
            }

            val paymentFlowResult = resultProcessor.process(SUCCEEDED)

            assertThat(stripeRepository.complete3ds2AuthInvocations)
                .isEqualTo(3)

            assertThat(paymentFlowResult)
                .isEqualTo(
                    PaymentFlowResult.Unvalidated(
                        clientSecret = "client_secret",
                        flowOutcome = StripeIntentResult.Outcome.SUCCEEDED,
                        stripeAccountId = "acct_123"
                    )
                )
        }

    @Test
    fun `complete3ds2Auth() should not retry after a 5xx response`() =
        runTest {
            stripeRepository.completionValue = {
                Result.failure(APIException(statusCode = 500))
            }

            val paymentFlowResult = resultProcessor.process(SUCCEEDED)

            assertThat(stripeRepository.complete3ds2AuthInvocations)
                .isEqualTo(1)

            assertThat(paymentFlowResult)
                .isEqualTo(
                    PaymentFlowResult.Unvalidated(
                        clientSecret = "client_secret",
                        flowOutcome = StripeIntentResult.Outcome.FAILED,
                        stripeAccountId = "acct_123"
                    )
                )
        }

    @Test
    fun `process() with Succeeded result should fire expected analytics events`() =
        runTest {
            resultProcessor.process(SUCCEEDED)

            assertThat(
                analyticsRequests.map { it.params["event"] }
            ).containsExactly(
                "stripe_android.3ds2_challenge_flow_completed",
                "stripe_android.3ds2_challenge_flow_presented"
            )
        }

    @Test
    fun `process() with Canceled result should fire expected analytics events`() =
        runTest {
            resultProcessor.process(
                ChallengeResult.Canceled(
                    UiType.Text.code,
                    UiType.Text,
                    INTENT_DATA
                )
            )

            assertThat(
                analyticsRequests.map { it.params["event"] }
            ).containsExactly(
                "stripe_android.3ds2_challenge_flow_canceled",
                "stripe_android.3ds2_challenge_flow_presented"
            )
        }

    @Test
    fun `process() with Failed result should fire expected analytics events`() =
        runTest {
            resultProcessor.process(
                ChallengeResult.Failed(
                    UiType.Text.code,
                    UiType.Text,
                    INTENT_DATA
                )
            )

            assertThat(
                analyticsRequests.map { it.params["event"] }
            ).containsExactly(
                "stripe_android.3ds2_challenge_flow_completed",
                "stripe_android.3ds2_challenge_flow_presented"
            )
        }

    @Test
    fun `process() with ProtocolError result should fire expected analytics events`() =
        runTest {
            resultProcessor.process(
                ChallengeResult.ProtocolError(
                    mock(),
                    UiType.Text,
                    INTENT_DATA
                )
            )

            assertThat(
                analyticsRequests.map { it.params["event"] }
            ).containsExactly(
                "stripe_android.3ds2_challenge_flow_errored",
                "stripe_android.3ds2_challenge_flow_presented"
            )
        }

    @Test
    fun `process() with Runtime result should fire expected analytics events`() =
        runTest {
            resultProcessor.process(
                ChallengeResult.RuntimeError(
                    RuntimeException(),
                    UiType.Text,
                    INTENT_DATA
                )
            )

            assertThat(
                analyticsRequests.map { it.params["event"] }
            ).containsExactly(
                "stripe_android.3ds2_challenge_flow_errored",
                "stripe_android.3ds2_challenge_flow_presented"
            )
        }

    @Test
    fun `process() with Timeout result should fire expected analytics events`() =
        runTest {
            resultProcessor.process(
                ChallengeResult.Timeout(
                    UiType.Text.code,
                    UiType.Text,
                    INTENT_DATA
                )
            )

            assertThat(
                analyticsRequests.map { it.params["event"] }
            ).containsExactly(
                "stripe_android.3ds2_challenge_flow_timed_out",
                "stripe_android.3ds2_challenge_flow_presented"
            )
        }

    private class FakeStripeRepository : AbsFakeStripeRepository() {
        val sourceIds = mutableListOf<String>()
        var complete3ds2AuthInvocations = 0

        var completionValue = { Result.success(Stripe3ds2AuthResultFixtures.CHALLENGE_COMPLETION) }

        override suspend fun complete3ds2Auth(
            sourceId: String,
            requestOptions: ApiRequest.Options
        ): Result<Stripe3ds2AuthResult> {
            complete3ds2AuthInvocations++
            sourceIds.add(sourceId)
            return completionValue()
        }
    }

    private companion object {
        private val INTENT_DATA = IntentData(
            "client_secret",
            "src_id",
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            "acct_123"
        )

        private val SUCCEEDED = ChallengeResult.Succeeded(
            UiType.Text.code,
            UiType.Text,
            INTENT_DATA
        )
    }
}
