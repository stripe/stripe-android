package com.stripe.android.payments

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.stripe.android.AnalyticsEvent
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
import com.stripe.android.networking.AnalyticsDataFactory
import com.stripe.android.networking.AnalyticsRequest
import com.stripe.android.networking.AnalyticsRequestExecutor
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.RetryDelaySupplier
import com.stripe.android.stripe3ds2.transaction.ChallengeFlowOutcome
import com.stripe.android.stripe3ds2.transaction.CompletionEvent
import com.stripe.android.stripe3ds2.transaction.ErrorMessage
import com.stripe.android.stripe3ds2.transaction.ProtocolErrorEvent
import com.stripe.android.stripe3ds2.transaction.RuntimeErrorEvent
import com.stripe.android.stripe3ds2.transaction.SdkTransactionId
import com.stripe.android.stripe3ds2.transaction.Transaction
import com.stripe.android.view.AuthActivityStarter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
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

    private val activity: Activity = mock()
    private val stripeRepository = FakeStripeRepository()

    private val host = AuthActivityStarter.Host.create(activity)
    private val completionStarter = Stripe3ds2CompletionStarter.Legacy(
        host,
        50000
    )
    private val analyticsDataFactory = AnalyticsDataFactory(
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

        val receiver = DefaultStripeChallengeStatusReceiver(
            completionStarter,
            stripeRepository,
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            "src_123",
            REQUEST_OPTIONS,
            analyticsRequestExecutor,
            analyticsDataFactory,
            transaction,
            AnalyticsRequest.Factory(),
            workContext = testDispatcher
        )
        receiver.completed(
            completionEvent,
            "01",
            ChallengeFlowOutcome.CompleteSuccessful
        )

        verify(analyticsRequestExecutor, times(2))
            .executeAsync(analyticsRequestArgumentCaptor.capture())
        val analyticsRequests = analyticsRequestArgumentCaptor.allValues

        assertThat(requireNotNull(analyticsRequests[0].params)[AnalyticsDataFactory.FIELD_EVENT])
            .isEqualTo(AnalyticsEvent.Auth3ds2ChallengeCompleted.toString())

        val analyticsParamsSecond = requireNotNull(analyticsRequests[1].params)
        assertThat(analyticsParamsSecond[AnalyticsDataFactory.FIELD_EVENT])
            .isEqualTo(AnalyticsEvent.Auth3ds2ChallengePresented.toString())
        assertThat(analyticsParamsSecond[AnalyticsDataFactory.FIELD_3DS2_UI_TYPE])
            .isEqualTo("oob")
    }

    @Test
    fun test3ds2Receiver_whenTimedout_shouldFireAnalyticsRequest() {
        val receiver = DefaultStripeChallengeStatusReceiver(
            completionStarter,
            stripeRepository,
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            "src_123",
            REQUEST_OPTIONS,
            analyticsRequestExecutor,
            analyticsDataFactory,
            transaction,
            AnalyticsRequest.Factory(),
            workContext = testDispatcher
        )
        receiver.timedout("01")
        verify(analyticsRequestExecutor, times(2))
            .executeAsync(analyticsRequestArgumentCaptor.capture())
        val analyticsRequests = analyticsRequestArgumentCaptor.allValues

        assertThat(requireNotNull(analyticsRequests[0].params)[AnalyticsDataFactory.FIELD_EVENT])
            .isEqualTo(AnalyticsEvent.Auth3ds2ChallengeTimedOut.toString())

        assertThat(requireNotNull(analyticsRequests[1].params)[AnalyticsDataFactory.FIELD_EVENT])
            .isEqualTo(AnalyticsEvent.Auth3ds2ChallengePresented.toString())
    }

    @Test
    fun test3ds2Receiver_whenCanceled_shouldFireAnalyticsRequest() {
        val receiver = DefaultStripeChallengeStatusReceiver(
            completionStarter,
            stripeRepository,
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            "src_123",
            REQUEST_OPTIONS,
            analyticsRequestExecutor,
            analyticsDataFactory,
            transaction,
            AnalyticsRequest.Factory(),
            workContext = testDispatcher
        )
        receiver.cancelled("01")

        verify(analyticsRequestExecutor, times(2))
            .executeAsync(analyticsRequestArgumentCaptor.capture())
        val analyticsRequests = analyticsRequestArgumentCaptor.allValues

        assertThat(requireNotNull(analyticsRequests[0].params)[AnalyticsDataFactory.FIELD_EVENT])
            .isEqualTo(AnalyticsEvent.Auth3ds2ChallengeCanceled.toString())

        assertThat(requireNotNull(analyticsRequests[1].params)[AnalyticsDataFactory.FIELD_EVENT])
            .isEqualTo(AnalyticsEvent.Auth3ds2ChallengePresented.toString())
    }

    @Test
    fun test3ds2Receiver_whenRuntimeErrorError_shouldFireAnalyticsRequest() {
        val runtimeErrorEvent = RuntimeErrorEvent(
            errorCode = "404",
            errorMessage = "Resource not found"
        )

        val receiver = DefaultStripeChallengeStatusReceiver(
            completionStarter,
            stripeRepository,
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            "src_123",
            REQUEST_OPTIONS,
            analyticsRequestExecutor,
            analyticsDataFactory,
            transaction,
            AnalyticsRequest.Factory(),
            workContext = testDispatcher
        )
        receiver.runtimeError(runtimeErrorEvent)

        verify(analyticsRequestExecutor, times(2))
            .executeAsync(analyticsRequestArgumentCaptor.capture())
        val analyticsRequests = analyticsRequestArgumentCaptor.allValues

        val analyticsParamsFirst = requireNotNull(analyticsRequests[0].params)
        assertThat(analyticsParamsFirst[AnalyticsDataFactory.FIELD_EVENT])
            .isEqualTo(AnalyticsEvent.Auth3ds2ChallengeErrored.toString())

        assertThat(analyticsParamsFirst[AnalyticsDataFactory.FIELD_ERROR_DATA])
            .isEqualTo(
                mapOf(
                    "type" to "runtime_error_event",
                    "error_code" to "404",
                    "error_message" to "Resource not found"
                )
            )

        assertThat(requireNotNull(analyticsRequests[1].params)[AnalyticsDataFactory.FIELD_EVENT])
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

        val receiver = DefaultStripeChallengeStatusReceiver(
            completionStarter,
            stripeRepository,
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            "src_123",
            REQUEST_OPTIONS,
            analyticsRequestExecutor,
            analyticsDataFactory,
            transaction,
            AnalyticsRequest.Factory(),
            workContext = testDispatcher
        )
        receiver.protocolError(protocolErrorEvent)

        verify(analyticsRequestExecutor, times(2))
            .executeAsync(analyticsRequestArgumentCaptor.capture())
        val analyticsRequests = analyticsRequestArgumentCaptor.allValues

        val analyticsParamsFirst = requireNotNull(analyticsRequests[0].params)
        assertThat(analyticsParamsFirst[AnalyticsDataFactory.FIELD_EVENT])
            .isEqualTo(AnalyticsEvent.Auth3ds2ChallengeErrored.toString())

        assertThat(analyticsParamsFirst[AnalyticsDataFactory.FIELD_ERROR_DATA])
            .isEqualTo(
                mapOf(
                    "type" to "protocol_error_event",
                    "error_code" to "201",
                    "sdk_trans_id" to sdkTransactionId.value,
                    "error_description" to "Required element missing",
                    "error_details" to "eci",
                    "trans_id" to "047f76a6-d1d4-48a2-aa65-786abb6f7f46"
                )
            )

        assertThat(requireNotNull(analyticsRequests[1].params)[AnalyticsDataFactory.FIELD_EVENT])
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
            analyticsDataFactory,
            transaction,
            AnalyticsRequest.Factory(),
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
            analyticsDataFactory,
            transaction,
            AnalyticsRequest.Factory(),

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
            analyticsDataFactory,
            transaction,
            AnalyticsRequest.Factory(),

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
            analyticsDataFactory,
            transaction,
            AnalyticsRequest.Factory(),

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
