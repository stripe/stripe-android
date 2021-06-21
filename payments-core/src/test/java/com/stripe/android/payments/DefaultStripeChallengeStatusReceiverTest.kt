package com.stripe.android.payments

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.StripeIntentResult
import com.stripe.android.StripePaymentController
import com.stripe.android.exception.APIException
import com.stripe.android.exception.InvalidRequestException
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.Source
import com.stripe.android.model.SourceFixtures
import com.stripe.android.model.Stripe3ds2AuthResult
import com.stripe.android.model.Stripe3ds2AuthResultFixtures
import com.stripe.android.model.Stripe3ds2Fixtures
import com.stripe.android.networking.AbsFakeStripeRepository
import com.stripe.android.networking.AnalyticsEvent
import com.stripe.android.networking.AnalyticsRequest
import com.stripe.android.networking.AnalyticsRequestExecutor
import com.stripe.android.networking.AnalyticsRequestFactory
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.RetryDelaySupplier
import com.stripe.android.stripe3ds2.transaction.ChallengeFlowOutcome
import com.stripe.android.stripe3ds2.transaction.CompletionEvent
import com.stripe.android.stripe3ds2.transaction.ErrorMessage
import com.stripe.android.stripe3ds2.transaction.ProtocolErrorEvent
import com.stripe.android.stripe3ds2.transaction.RuntimeErrorEvent
import com.stripe.android.stripe3ds2.transaction.SdkTransactionId
import com.stripe.android.stripe3ds2.transaction.Transaction
import com.stripe.android.view.AuthActivityStarterHost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
class DefaultStripeChallengeStatusReceiverTest {
    private val testDispatcher = TestCoroutineDispatcher()
    private val context: Context = ApplicationProvider.getApplicationContext()

    private val sdkTransactionId = mock<SdkTransactionId>().also {
        whenever(it.value).thenReturn(UUID.randomUUID().toString())
    }
    private val transaction = mock<Transaction>().also {
        whenever(it.sdkTransactionId)
            .thenReturn(sdkTransactionId)
    }
    private val analyticsRequestExecutor: AnalyticsRequestExecutor = mock()

    private val analyticsRequestArgumentCaptor = argumentCaptor<AnalyticsRequest>()
    private val intentArgumentCaptor = argumentCaptor<Intent>()

    private val activity: ComponentActivity = mock()
    private val stripeRepository = FakeStripeRepository()

    private val host = AuthActivityStarterHost.create(activity)
    private val completionStarter = Stripe3ds2CompletionStarter.Legacy(
        host,
        50000
    )
    private val analyticsRequestFactory = AnalyticsRequestFactory(
        context,
        ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
    )

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        runBlocking {
            whenever(transaction.createAuthenticationRequestParameters())
                .thenReturn(Stripe3ds2Fixtures.createAreqParams(sdkTransactionId))
        }
        whenever(activity.applicationContext)
            .thenReturn(context)
    }

    @AfterTest
    fun cleanup() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun test3ds2Receiver_whenCompleted_shouldFireAnalyticsRequest() {
        val completionEvent = CompletionEvent(
            sdkTransactionId = sdkTransactionId,
            transactionStatus = "C"
        )

        whenever(transaction.initialChallengeUiType).thenReturn("04")

        var endCalled = false
        val receiver = DefaultStripeChallengeStatusReceiver(
            completionStarter,
            stripeRepository,
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            "src_123",
            REQUEST_OPTIONS,
            analyticsRequestExecutor,
            analyticsRequestFactory,
            transaction,
            onEndChallenge = {
                endCalled = true
            },
            workContext = testDispatcher
        )
        receiver.completed(
            completionEvent,
            "01",
            ChallengeFlowOutcome.CompleteSuccessful
        )

        assertThat(endCalled).isTrue()
        verify(analyticsRequestExecutor, times(2))
            .executeAsync(analyticsRequestArgumentCaptor.capture())
        val analyticsRequests = analyticsRequestArgumentCaptor.allValues

        assertThat(requireNotNull(analyticsRequests[0].params)[AnalyticsRequestFactory.FIELD_EVENT])
            .isEqualTo(AnalyticsEvent.Auth3ds2ChallengeCompleted.toString())

        val analyticsParamsSecond = requireNotNull(analyticsRequests[1].params)
        assertThat(analyticsParamsSecond[AnalyticsRequestFactory.FIELD_EVENT])
            .isEqualTo(AnalyticsEvent.Auth3ds2ChallengePresented.toString())
        assertThat(analyticsParamsSecond[AnalyticsRequestFactory.FIELD_3DS2_UI_TYPE])
            .isEqualTo("oob")
    }

    @Test
    fun test3ds2Receiver_whenTimedout_shouldFireAnalyticsRequest() {
        var endCalled = false
        val receiver = DefaultStripeChallengeStatusReceiver(
            completionStarter,
            stripeRepository,
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            "src_123",
            REQUEST_OPTIONS,
            analyticsRequestExecutor,
            analyticsRequestFactory,
            transaction,
            onEndChallenge = {
                endCalled = true
            },
            workContext = testDispatcher
        )
        receiver.timedout("01")

        assertThat(endCalled).isTrue()
        verify(analyticsRequestExecutor, times(2))
            .executeAsync(analyticsRequestArgumentCaptor.capture())
        val analyticsRequests = analyticsRequestArgumentCaptor.allValues

        assertThat(requireNotNull(analyticsRequests[0].params)[AnalyticsRequestFactory.FIELD_EVENT])
            .isEqualTo(AnalyticsEvent.Auth3ds2ChallengeTimedOut.toString())

        assertThat(requireNotNull(analyticsRequests[1].params)[AnalyticsRequestFactory.FIELD_EVENT])
            .isEqualTo(AnalyticsEvent.Auth3ds2ChallengePresented.toString())
    }

    @Test
    fun test3ds2Receiver_whenCanceled_shouldFireAnalyticsRequest() {
        var endCalled = false
        val receiver = DefaultStripeChallengeStatusReceiver(
            completionStarter,
            stripeRepository,
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            "src_123",
            REQUEST_OPTIONS,
            analyticsRequestExecutor,
            analyticsRequestFactory,
            transaction,
            onEndChallenge = {
                endCalled = true
            },
            workContext = testDispatcher
        )
        receiver.cancelled("01")

        assertThat(endCalled).isTrue()
        verify(analyticsRequestExecutor, times(2))
            .executeAsync(analyticsRequestArgumentCaptor.capture())
        val analyticsRequests = analyticsRequestArgumentCaptor.allValues

        assertThat(requireNotNull(analyticsRequests[0].params)[AnalyticsRequestFactory.FIELD_EVENT])
            .isEqualTo(AnalyticsEvent.Auth3ds2ChallengeCanceled.toString())

        assertThat(requireNotNull(analyticsRequests[1].params)[AnalyticsRequestFactory.FIELD_EVENT])
            .isEqualTo(AnalyticsEvent.Auth3ds2ChallengePresented.toString())
    }

    @Test
    fun test3ds2Receiver_whenRuntimeErrorError_shouldFireAnalyticsRequest() {
        val runtimeErrorEvent = RuntimeErrorEvent(
            errorCode = "404",
            errorMessage = "Resource not found"
        )

        var endCalled = false
        val receiver = DefaultStripeChallengeStatusReceiver(
            completionStarter,
            stripeRepository,
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            "src_123",
            REQUEST_OPTIONS,
            analyticsRequestExecutor,
            analyticsRequestFactory,
            transaction,
            onEndChallenge = {
                endCalled = true
            },
            workContext = testDispatcher
        )
        receiver.runtimeError(runtimeErrorEvent)

        assertThat(endCalled).isTrue()
        verify(analyticsRequestExecutor, times(2))
            .executeAsync(analyticsRequestArgumentCaptor.capture())
        val analyticsRequests = analyticsRequestArgumentCaptor.allValues

        val analyticsParamsFirst = requireNotNull(analyticsRequests[0].params)
        assertThat(analyticsParamsFirst[AnalyticsRequestFactory.FIELD_EVENT])
            .isEqualTo(AnalyticsEvent.Auth3ds2ChallengeErrored.toString())

        assertThat(requireNotNull(analyticsRequests[1].params)[AnalyticsRequestFactory.FIELD_EVENT])
            .isEqualTo(AnalyticsEvent.Auth3ds2ChallengePresented.toString())
    }

    @Test
    fun test3ds2Receiver_whenProtocolError_shouldFireAnalyticsRequest() {
        val protocolErrorEvent = ProtocolErrorEvent(
            sdkTransactionId = sdkTransactionId,
            errorMessage = ErrorMessage(
                errorCode = "201",
                errorDescription = "Required element missing",
                errorDetails = "eci",
                transactionId = "047f76a6-d1d4-48a2-aa65-786abb6f7f46"
            )
        )

        var endCalled = false
        val receiver = DefaultStripeChallengeStatusReceiver(
            completionStarter,
            stripeRepository,
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            "src_123",
            REQUEST_OPTIONS,
            analyticsRequestExecutor,
            analyticsRequestFactory,
            transaction,
            onEndChallenge = {
                endCalled = true
            },
            workContext = testDispatcher
        )
        receiver.protocolError(protocolErrorEvent)

        assertThat(endCalled).isTrue()
        verify(analyticsRequestExecutor, times(2))
            .executeAsync(analyticsRequestArgumentCaptor.capture())
        val analyticsRequests = analyticsRequestArgumentCaptor.allValues

        val analyticsParamsFirst = requireNotNull(analyticsRequests[0].params)
        assertThat(analyticsParamsFirst[AnalyticsRequestFactory.FIELD_EVENT])
            .isEqualTo(AnalyticsEvent.Auth3ds2ChallengeErrored.toString())

        assertThat(requireNotNull(analyticsRequests[1].params)[AnalyticsRequestFactory.FIELD_EVENT])
            .isEqualTo(AnalyticsEvent.Auth3ds2ChallengePresented.toString())
    }

    @Test
    fun test3ds2Completion_whenCanceled_shouldCallStarterWithCancelStatus() {
        val receiver = DefaultStripeChallengeStatusReceiver(
            completionStarter,
            stripeRepository,
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            "src_123",
            REQUEST_OPTIONS,
            analyticsRequestExecutor,
            analyticsRequestFactory,
            transaction,
            onEndChallenge = {},
            workContext = testDispatcher
        )
        receiver.cancelled("01")

        verify(activity).startActivityForResult(
            intentArgumentCaptor.capture(),
            eq(50000)
        )

        val paymentFlowResult = Stripe3ds2CompletionContract().parsePaymentFlowResult(
            intentArgumentCaptor.firstValue
        )

        assertThat(paymentFlowResult)
            .isEqualTo(
                PaymentFlowResult.Unvalidated(
                    clientSecret = PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2.clientSecret.orEmpty(),
                    stripeAccountId = ACCOUNT_ID,
                    flowOutcome = StripeIntentResult.Outcome.CANCELED
                )
            )
    }

    @Test
    fun `complete3ds2Auth() should retry until max retries are attempted due to a 4xx response`() {
        var complete3ds2AuthInvocations = 0
        val stripeRepository = object : AbsFakeStripeRepository() {
            override suspend fun complete3ds2Auth(
                sourceId: String,
                requestOptions: ApiRequest.Options
            ): Stripe3ds2AuthResult {
                complete3ds2AuthInvocations++

                throw InvalidRequestException(
                    statusCode = 400
                )
            }
        }

        val receiver = DefaultStripeChallengeStatusReceiver(
            completionStarter,
            stripeRepository,
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            "src_123",
            REQUEST_OPTIONS,
            analyticsRequestExecutor,
            analyticsRequestFactory,
            transaction,
            onEndChallenge = {},

            // set to 0 so there is effectively no delay
            retryDelaySupplier = RetryDelaySupplier(incrementSeconds = 0L),

            workContext = testDispatcher
        )

        receiver.timedout("1")

        assertThat(complete3ds2AuthInvocations)
            .isEqualTo(4)
    }

    @Test
    fun `complete3ds2Auth() should succeed after a single retry failure due to a 4xx response`() {
        var complete3ds2AuthInvocations = 0

        val stripeRepository = object : AbsFakeStripeRepository() {
            override suspend fun complete3ds2Auth(
                sourceId: String,
                requestOptions: ApiRequest.Options
            ): Stripe3ds2AuthResult {
                complete3ds2AuthInvocations++
                if (complete3ds2AuthInvocations <= 2) {
                    throw InvalidRequestException(
                        statusCode = 400
                    )
                } else {
                    return Stripe3ds2AuthResultFixtures.CHALLENGE_COMPLETION
                }
            }
        }

        val receiver = DefaultStripeChallengeStatusReceiver(
            completionStarter,
            stripeRepository,
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            "src_123",
            REQUEST_OPTIONS,
            analyticsRequestExecutor,
            analyticsRequestFactory,
            transaction,
            onEndChallenge = {},

            // set to 0 so there is effectively no delay
            retryDelaySupplier = RetryDelaySupplier(incrementSeconds = 0L),

            workContext = testDispatcher
        )

        receiver.timedout("1")

        assertThat(complete3ds2AuthInvocations)
            .isEqualTo(3)

        verify(activity).startActivityForResult(
            argWhere { intent ->
                val result = requireNotNull(
                    Stripe3ds2CompletionContract().parsePaymentFlowResult(intent)
                )
                result == PaymentFlowResult.Unvalidated(
                    clientSecret = "pi_1ExkUeAWhjPjYwPiXph9ouXa_secret_nGTdfGlzL9Uop59wN55LraiC7",
                    flowOutcome = StripeIntentResult.Outcome.TIMEDOUT,
                    stripeAccountId = ACCOUNT_ID
                )
            },
            eq(StripePaymentController.PAYMENT_REQUEST_CODE)
        )
    }

    @Test
    fun `complete3ds2Auth() should not retry after a 5xx response`() {
        var complete3ds2AuthInvocations = 0
        val stripeRepository = object : AbsFakeStripeRepository() {
            override suspend fun complete3ds2Auth(
                sourceId: String,
                requestOptions: ApiRequest.Options
            ): Stripe3ds2AuthResult {
                complete3ds2AuthInvocations++

                throw APIException(
                    statusCode = 500
                )
            }
        }

        val receiver = DefaultStripeChallengeStatusReceiver(
            completionStarter,
            stripeRepository,
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            "src_123",
            REQUEST_OPTIONS,
            analyticsRequestExecutor,
            analyticsRequestFactory,
            transaction,
            onEndChallenge = {},

            // set to 0 so there is effectively no delay
            retryDelaySupplier = RetryDelaySupplier(incrementSeconds = 0L),

            workContext = testDispatcher
        )

        receiver.timedout("1")

        assertThat(complete3ds2AuthInvocations)
            .isEqualTo(1)
    }

    private class FakeStripeRepository : AbsFakeStripeRepository() {
        override suspend fun retrieveSetupIntent(
            clientSecret: String,
            options: ApiRequest.Options,
            expandFields: List<String>
        ) = SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT

        override suspend fun retrievePaymentIntent(
            clientSecret: String,
            options: ApiRequest.Options,
            expandFields: List<String>
        ) = PaymentIntentFixtures.PI_REQUIRES_REDIRECT

        override suspend fun retrieveSource(
            sourceId: String,
            clientSecret: String,
            options: ApiRequest.Options
        ) = SourceFixtures.SOURCE_CARD.copy(status = Source.Status.Chargeable)

        override suspend fun cancelPaymentIntentSource(
            paymentIntentId: String,
            sourceId: String,
            options: ApiRequest.Options
        ) = PaymentIntentFixtures.CANCELLED

        override suspend fun cancelSetupIntentSource(
            setupIntentId: String,
            sourceId: String,
            options: ApiRequest.Options
        ) = SetupIntentFixtures.CANCELLED
    }

    private companion object {
        private const val ACCOUNT_ID = "acct_123"

        private val REQUEST_OPTIONS = ApiRequest.Options(
            apiKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            stripeAccount = ACCOUNT_ID
        )
    }
}
