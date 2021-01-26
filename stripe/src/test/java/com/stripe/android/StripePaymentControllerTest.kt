package com.stripe.android

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import com.stripe.android.auth.PaymentAuthWebViewContract
import com.stripe.android.exception.APIException
import com.stripe.android.exception.InvalidRequestException
import com.stripe.android.model.AlipayAuthResult
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.Source
import com.stripe.android.model.SourceFixtures
import com.stripe.android.model.Stripe3ds2AuthResultFixtures
import com.stripe.android.model.Stripe3ds2Fingerprint
import com.stripe.android.model.Stripe3ds2FingerprintTest
import com.stripe.android.model.Stripe3ds2Fixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.AbsFakeStripeRepository
import com.stripe.android.networking.AlipayRepository
import com.stripe.android.networking.AnalyticsDataFactory
import com.stripe.android.networking.AnalyticsRequest
import com.stripe.android.networking.AnalyticsRequestExecutor
import com.stripe.android.networking.ApiRequest
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.payments.Stripe3ds2CompletionContract
import com.stripe.android.payments.Stripe3ds2CompletionStarter
import com.stripe.android.stripe3ds2.service.StripeThreeDs2Service
import com.stripe.android.stripe3ds2.transaction.ChallengeFlowOutcome
import com.stripe.android.stripe3ds2.transaction.ChallengeParameters
import com.stripe.android.stripe3ds2.transaction.CompletionEvent
import com.stripe.android.stripe3ds2.transaction.ErrorMessage
import com.stripe.android.stripe3ds2.transaction.MessageVersionRegistry
import com.stripe.android.stripe3ds2.transaction.ProtocolErrorEvent
import com.stripe.android.stripe3ds2.transaction.RuntimeErrorEvent
import com.stripe.android.stripe3ds2.transaction.SdkTransactionId
import com.stripe.android.stripe3ds2.transaction.Stripe3ds2ActivityStarterHost
import com.stripe.android.stripe3ds2.transaction.Transaction
import com.stripe.android.utils.ParcelUtils
import com.stripe.android.view.AuthActivityStarter
import com.stripe.android.view.PaymentRelayActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
internal class StripePaymentControllerTest {

    private val activity: Activity = mock()
    private val threeDs2Service: StripeThreeDs2Service = mock()
    private val sdkTransactionId = mock<SdkTransactionId>().also {
        whenever(it.value).thenReturn(UUID.randomUUID().toString())
    }
    private val transaction: Transaction = mock<Transaction>().also {
        whenever(it.sdkTransactionId)
            .thenReturn(sdkTransactionId)
    }
    private val paymentIntentResultCallback: ApiResultCallback<PaymentIntentResult> = mock()
    private val setupIntentResultCallback: ApiResultCallback<SetupIntentResult> = mock()
    private val sourceCallback: ApiResultCallback<Source> = mock()
    private val paymentRelayStarter: PaymentRelayStarter = mock()
    private val analyticsRequestExecutor: AnalyticsRequestExecutor = mock()
    private val challengeProgressActivityStarter: StripePaymentController.ChallengeProgressActivityStarter = mock()
    private val alipayRepository = FakeAlipayRepostiory()
    private val stripeRepository = FakeStripeRepository()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val analyticsDataFactory = AnalyticsDataFactory(
        context,
        ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
    )
    private val host = AuthActivityStarter.Host.create(activity)

    private val relayStarterArgsArgumentCaptor: KArgumentCaptor<PaymentRelayStarter.Args> = argumentCaptor()
    private val intentArgumentCaptor: KArgumentCaptor<Intent> = argumentCaptor()
    private val analyticsRequestArgumentCaptor: KArgumentCaptor<AnalyticsRequest> = argumentCaptor()
    private val setupIntentResultArgumentCaptor: KArgumentCaptor<SetupIntentResult> = argumentCaptor()
    private val sourceArgumentCaptor: KArgumentCaptor<Source> = argumentCaptor()

    private val testDispatcher = TestCoroutineDispatcher()

    private val paymentAuthWebViewContract = PaymentAuthWebViewContract()
    private val controller = createController()

    private val completionStarter = Stripe3ds2CompletionStarter.Legacy(
        host,
        50000
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
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    internal fun handleNextAction_withMastercardAnd3ds2_shouldStart3ds2ChallengeFlow() = testDispatcher.runBlockingTest {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2
        val dsPublicKey = Stripe3ds2Fingerprint(paymentIntent.nextActionData as StripeIntent.NextActionData.SdkData.Use3DS2)
            .directoryServerEncryption
            .directoryServerPublicKey
        whenever(
            threeDs2Service.createTransaction(
                eq(MASTERCARD_DS_ID),
                eq(MESSAGE_VERSION),
                eq(paymentIntent.isLiveMode),
                eq("mastercard"),
                any(),
                eq(dsPublicKey),
                eq("7c4debe3f4af7f9d1569a2ffea4343c2566826ee")
            )
        )
            .thenReturn(transaction)
        controller.handleNextAction(host, paymentIntent, REQUEST_OPTIONS)
        testDispatcher.advanceTimeBy(StripePaymentController.CHALLENGE_DELAY)

        verify(threeDs2Service).createTransaction(
            eq(MASTERCARD_DS_ID),
            eq(MESSAGE_VERSION),
            eq(paymentIntent.isLiveMode),
            eq("mastercard"),
            any(),
            eq(dsPublicKey),
            eq("7c4debe3f4af7f9d1569a2ffea4343c2566826ee")
        )
        verify(transaction)
            .doChallenge(any<Stripe3ds2ActivityStarterHost>(), any(), any(), any())

        verify(challengeProgressActivityStarter).start(
            eq(activity),
            eq("mastercard"),
            eq(false),
            any(),
            eq(sdkTransactionId)
        )

        verify(analyticsRequestExecutor)
            .executeAsync(analyticsRequestArgumentCaptor.capture())
        val analyticsParams = requireNotNull(analyticsRequestArgumentCaptor.firstValue.params)
        assertThat(analyticsParams[AnalyticsDataFactory.FIELD_EVENT])
            .isEqualTo(AnalyticsEvent.Auth3ds2Fingerprint.toString())
        assertThat(analyticsParams[AnalyticsDataFactory.FIELD_INTENT_ID])
            .isEqualTo(PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2.id)
    }

    @Test
    fun handleNextAction_withAmexAnd3ds2_shouldStart3ds2ChallengeFlow() = testDispatcher.runBlockingTest {
        whenever(
            threeDs2Service.createTransaction(
                eq(AMEX_DS_ID),
                eq(MESSAGE_VERSION),
                eq(PaymentIntentFixtures.PI_REQUIRES_AMEX_3DS2.isLiveMode),
                eq("american_express"),
                any(),
                eq(Stripe3ds2FingerprintTest.DS_RSA_PUBLIC_KEY),
                eq(PaymentIntentFixtures.KEY_ID)
            )
        )
            .thenReturn(transaction)
        controller.handleNextAction(
            host,
            PaymentIntentFixtures.PI_REQUIRES_AMEX_3DS2,
            REQUEST_OPTIONS
        )
        testDispatcher.advanceTimeBy(StripePaymentController.CHALLENGE_DELAY)

        verify(threeDs2Service).createTransaction(
            eq(AMEX_DS_ID),
            eq(MESSAGE_VERSION),
            eq(PaymentIntentFixtures.PI_REQUIRES_AMEX_3DS2.isLiveMode),
            eq("american_express"),
            any(),
            eq(Stripe3ds2FingerprintTest.DS_RSA_PUBLIC_KEY),
            eq(PaymentIntentFixtures.KEY_ID)
        )
        verify(transaction)
            .doChallenge(any<Stripe3ds2ActivityStarterHost>(), any(), any(), any())

        verify(challengeProgressActivityStarter).start(
            eq(activity),
            eq("american_express"),
            eq(false),
            any(),
            eq(sdkTransactionId)
        )
    }

    @Test
    fun handleNextAction_whenSdk3ds1() {
        controller.handleNextAction(
            host,
            PaymentIntentFixtures.PI_REQUIRES_3DS1,
            REQUEST_OPTIONS
        )
        verify(activity).startActivityForResult(
            intentArgumentCaptor.capture(),
            eq(StripePaymentController.PAYMENT_REQUEST_CODE)
        )
        val args = requireNotNull(
            paymentAuthWebViewContract.parseArgs(intentArgumentCaptor.firstValue)
        )
        assertThat(args.url)
            .isEqualTo("https://hooks.stripe.com/3d_secure_2_eap/begin_test/src_1Ecve7CRMbs6FrXfm8AxXMIh/src_client_secret_F79yszOBAiuaZTuIhbn3LPUW")
        assertThat(args.returnUrl).isNull()

        verifyAnalytics(AnalyticsEvent.Auth3ds1Sdk)
    }

    @Test
    fun handleNextAction_whenBrowser3ds1() {
        controller.handleNextAction(
            host,
            PaymentIntentFixtures.PI_REQUIRES_REDIRECT,
            REQUEST_OPTIONS
        )
        verify(activity).startActivityForResult(
            intentArgumentCaptor.capture(),
            eq(StripePaymentController.PAYMENT_REQUEST_CODE)
        )
        val args = requireNotNull(
            paymentAuthWebViewContract.parseArgs(intentArgumentCaptor.firstValue)
        )
        assertThat(args.url)
            .isEqualTo("https://hooks.stripe.com/3d_secure_2_eap/begin_test/src_1Ecaz6CRMbs6FrXfuYKBRSUG/src_client_secret_F6octeOshkgxT47dr0ZxSZiv")
        assertThat(args.returnUrl).isEqualTo("stripe://deeplink")

        verify(analyticsRequestExecutor)
            .executeAsync(analyticsRequestArgumentCaptor.capture())
        val analyticsParams = requireNotNull(analyticsRequestArgumentCaptor.firstValue.params)
        assertThat(analyticsParams[AnalyticsDataFactory.FIELD_EVENT])
            .isEqualTo(AnalyticsEvent.AuthRedirect.toString())
        assertThat(analyticsParams[AnalyticsDataFactory.FIELD_INTENT_ID])
            .isEqualTo("pi_1EZlvVCRMbs6FrXfKpq2xMmy")
    }

    @Test
    fun handleNextAction_when3dsRedirectWithSetupIntent() {
        controller.handleNextAction(
            host,
            SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT,
            REQUEST_OPTIONS
        )
        verify(activity).startActivityForResult(
            any(),
            eq(StripePaymentController.SETUP_REQUEST_CODE)
        )
    }

    @Test
    fun `handleNextAction oxxo details`() {
        controller.handleNextAction(
            host,
            PaymentIntentFixtures.OXXO_REQUIES_ACTION,
            REQUEST_OPTIONS
        )
        verify(activity).startActivityForResult(
            intentArgumentCaptor.capture(),
            eq(StripePaymentController.PAYMENT_REQUEST_CODE)
        )
        val args = requireNotNull(
            paymentAuthWebViewContract.parseArgs(intentArgumentCaptor.firstValue)
        )
        assertThat(args.url)
            .isEqualTo("https://payments.stripe.com/oxxo/voucher/vchr_test_YWNjdF8xR1hhNUZIU0wxMEo5d3F2LHZjaHJfSGJIOGVMYmNmQlkyMUJ5OU1WTU5uMVYxdDNta1Q2RQ0000gtenGCef")
        assertThat(args.shouldCancelIntentOnUserNavigation).isFalse()
    }

    @Test
    fun shouldHandleResult_withInvalidResultCode() {
        assertThat(controller.shouldHandlePaymentResult(500, Intent())).isFalse()
        assertThat(controller.shouldHandleSetupResult(500, Intent())).isFalse()
    }

    @Test
    fun getRequestCode_withIntents_correctCodeReturned() {
        assertThat(StripePaymentController.getRequestCode(PaymentIntentFixtures.PI_REQUIRES_3DS1))
            .isEqualTo(StripePaymentController.PAYMENT_REQUEST_CODE)
        assertThat(StripePaymentController.getRequestCode(SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT))
            .isEqualTo(StripePaymentController.SETUP_REQUEST_CODE)
    }

    @Test
    fun getRequestCode_withParams_correctCodeReturned() {
        assertThat(
            StripePaymentController.getRequestCode(
                ConfirmPaymentIntentParams.createWithPaymentMethodId(
                    "pm_123",
                    "client_secret",
                    ""
                )
            )
        ).isEqualTo(StripePaymentController.PAYMENT_REQUEST_CODE)
    }

    @Test
    fun test3ds2Receiver_whenCompleted_shouldFireAnalyticsRequest() {
        val completionEvent = CompletionEvent(
            sdkTransactionId = sdkTransactionId,
            transactionStatus = "C"
        )

        whenever(transaction.initialChallengeUiType).thenReturn("04")

        val receiver = StripePaymentController.PaymentAuth3ds2ChallengeStatusReceiver(
            completionStarter,
            stripeRepository,
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            "src_123",
            REQUEST_OPTIONS,
            analyticsRequestExecutor,
            analyticsDataFactory,
            transaction,
            AnalyticsRequest.Factory(),
            testDispatcher
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
        val receiver = StripePaymentController.PaymentAuth3ds2ChallengeStatusReceiver(
            completionStarter,
            stripeRepository,
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            "src_123",
            REQUEST_OPTIONS,
            analyticsRequestExecutor,
            analyticsDataFactory,
            transaction,
            AnalyticsRequest.Factory(),
            testDispatcher
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
        val receiver = StripePaymentController.PaymentAuth3ds2ChallengeStatusReceiver(
            completionStarter,
            stripeRepository,
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            "src_123",
            REQUEST_OPTIONS,
            analyticsRequestExecutor,
            analyticsDataFactory,
            transaction,
            AnalyticsRequest.Factory(),
            testDispatcher
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

        val receiver = StripePaymentController.PaymentAuth3ds2ChallengeStatusReceiver(
            completionStarter,
            stripeRepository,
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            "src_123",
            REQUEST_OPTIONS,
            analyticsRequestExecutor,
            analyticsDataFactory,
            transaction,
            AnalyticsRequest.Factory(),
            testDispatcher
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

        val receiver = StripePaymentController.PaymentAuth3ds2ChallengeStatusReceiver(
            completionStarter,
            stripeRepository,
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            "src_123",
            REQUEST_OPTIONS,
            analyticsRequestExecutor,
            analyticsDataFactory,
            transaction,
            AnalyticsRequest.Factory(),
            testDispatcher
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
        Dispatchers.setMain(testDispatcher)

        val receiver = StripePaymentController.PaymentAuth3ds2ChallengeStatusReceiver(
            completionStarter,
            stripeRepository,
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            "src_123",
            REQUEST_OPTIONS,
            analyticsRequestExecutor,
            analyticsDataFactory,
            transaction,
            AnalyticsRequest.Factory(),
            testDispatcher
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
    fun handlePaymentResult_withAuthException_shouldCallCallbackOnError() {
        val exception = APIException(RuntimeException())
        val intent = Intent().putExtras(
            PaymentFlowResult.Unvalidated(
                exception = exception
            ).toBundle()
        )

        controller.handlePaymentResult(intent, paymentIntentResultCallback)
        verify(paymentIntentResultCallback).onError(exception)
        verify(paymentIntentResultCallback, never())
            .onSuccess(anyOrNull())
    }

    @Test
    fun handleSetupResult_withAuthException_shouldCallCallbackOnError() {
        val exception = APIException(RuntimeException())
        val intent = Intent().putExtras(
            PaymentFlowResult.Unvalidated(
                exception = exception
            ).toBundle()
        )

        controller.handleSetupResult(intent, setupIntentResultCallback)

        verify(setupIntentResultCallback).onError(exception)
        verify(setupIntentResultCallback, never())
            .onSuccess(anyOrNull())
    }

    @Test
    fun handleSetupResult_shouldCallbackOnSuccess() {
        assertThat(SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT.clientSecret)
            .isNotNull()

        val intent = Intent().putExtras(
            PaymentFlowResult.Unvalidated(
                clientSecret = SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT.clientSecret,
                flowOutcome = StripeIntentResult.Outcome.SUCCEEDED
            ).toBundle()
        )

        controller.handleSetupResult(intent, setupIntentResultCallback)

        verify(setupIntentResultCallback)
            .onSuccess(setupIntentResultArgumentCaptor.capture())
        val result = setupIntentResultArgumentCaptor.firstValue
        assertThat(result.outcome).isEqualTo(StripeIntentResult.Outcome.SUCCEEDED)
        assertThat(result.intent).isEqualTo(SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT)
    }

    @Test
    fun `on3ds2AuthSuccess() with challenge flow should not start relay activity`() = testDispatcher.runBlockingTest {
        controller.on3ds2AuthSuccess(
            Stripe3ds2AuthResultFixtures.ARES_CHALLENGE_FLOW,
            transaction,
            SOURCE_ID,
            MAX_TIMEOUT,
            paymentRelayStarter,
            StripePaymentController.PAYMENT_REQUEST_CODE,
            host,
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            REQUEST_OPTIONS
        )
        verify(paymentRelayStarter, never())
            .start(anyOrNull())
    }

    @Test
    fun `on3ds2AuthSuccess() with frictionless flow should start relay activity with PaymentIntent`() = testDispatcher.runBlockingTest {
        controller.on3ds2AuthSuccess(
            Stripe3ds2AuthResultFixtures.ARES_FRICTIONLESS_FLOW,
            transaction,
            SOURCE_ID,
            MAX_TIMEOUT,
            paymentRelayStarter,
            StripePaymentController.PAYMENT_REQUEST_CODE,
            host,
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            REQUEST_OPTIONS
        )
        verify(paymentRelayStarter)
            .start(relayStarterArgsArgumentCaptor.capture())
        val args = relayStarterArgsArgumentCaptor.firstValue as? PaymentRelayStarter.Args.PaymentIntentArgs
        assertThat(args?.paymentIntent)
            .isEqualTo(PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2)

        verify(analyticsRequestExecutor)
            .executeAsync(analyticsRequestArgumentCaptor.capture())
        val analyticsRequest = analyticsRequestArgumentCaptor.firstValue
        val analyticsParams = requireNotNull(analyticsRequest.params)
        assertThat(analyticsParams[AnalyticsDataFactory.FIELD_EVENT])
            .isEqualTo(AnalyticsEvent.Auth3ds2Frictionless.toString())
        assertThat(analyticsParams[AnalyticsDataFactory.FIELD_INTENT_ID]).isEqualTo("pi_1ExkUeAWhjPjYwPiXph9ouXa")
    }

    @Test
    fun `on3ds2AuthSuccess() with fallback redirect URL should start auth webview activity`() = testDispatcher.runBlockingTest {
        controller.on3ds2AuthSuccess(
            Stripe3ds2AuthResultFixtures.FALLBACK_REDIRECT_URL,
            transaction,
            SOURCE_ID,
            MAX_TIMEOUT,
            paymentRelayStarter,
            StripePaymentController.PAYMENT_REQUEST_CODE,
            host,
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            REQUEST_OPTIONS
        )
        verify(activity).startActivityForResult(
            intentArgumentCaptor.capture(),
            eq(StripePaymentController.PAYMENT_REQUEST_CODE)
        )
        val args = requireNotNull(
            paymentAuthWebViewContract.parseArgs(intentArgumentCaptor.firstValue)
        )
        assertThat(args.url)
            .isEqualTo("https://hooks.stripe.com/3d_secure_2_eap/begin_test/src_1Ecve7CRMbs6FrXfm8AxXMIh/src_client_secret_F79yszOBAiuaZTuIhbn3LPUW")
        assertThat(args.returnUrl).isNull()

        verify(analyticsRequestExecutor)
            .executeAsync(analyticsRequestArgumentCaptor.capture())
        val analyticsRequest = analyticsRequestArgumentCaptor.firstValue
        assertThat(
            requireNotNull(analyticsRequest.params)[AnalyticsDataFactory.FIELD_EVENT]
        ).isEqualTo(AnalyticsEvent.Auth3ds2Fallback.toString())
    }

    @Test
    fun `on3ds2AuthSuccess() with AReq error should start relay activity with exception`() = testDispatcher.runBlockingTest {
        controller.on3ds2AuthSuccess(
            Stripe3ds2AuthResultFixtures.ERROR,
            transaction,
            SOURCE_ID,
            MAX_TIMEOUT,
            paymentRelayStarter,
            StripePaymentController.PAYMENT_REQUEST_CODE,
            host,
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            REQUEST_OPTIONS
        )

        verify(paymentRelayStarter)
            .start(relayStarterArgsArgumentCaptor.capture())

        val args = relayStarterArgsArgumentCaptor.firstValue as? PaymentRelayStarter.Args.ErrorArgs
        assertThat(args?.exception?.message).isEqualTo(
            "Error encountered during 3DS2 authentication request. " +
                "Code: 302, Detail: null, " +
                "Description: Data could not be decrypted by the receiving system due to " +
                "technical or other reason., Component: D"
        )
    }

    @Test
    fun `startChallengeFlow() when successful should call doChallenge()`() = testDispatcher.runBlockingTest {
        val ares = requireNotNull(Stripe3ds2AuthResultFixtures.ARES_CHALLENGE_FLOW.ares)
        controller.startChallengeFlow(
            ares,
            transaction,
            SOURCE_ID,
            MAX_TIMEOUT,
            paymentRelayStarter,
            host,
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            REQUEST_OPTIONS
        )

        testDispatcher.advanceTimeBy(StripePaymentController.CHALLENGE_DELAY)

        verify(transaction).doChallenge(
            any<Stripe3ds2ActivityStarterHost>(),
            eq(
                ChallengeParameters(
                    acsSignedContent = null,
                    threeDsServerTransactionId = ares.threeDSServerTransId,
                    acsTransactionId = ares.acsTransId
                )
            ),
            any(),
            eq(MAX_TIMEOUT)
        )
    }

    @Test
    fun `startChallengeFlow() when failure should start relay activity with exception()`() = testDispatcher.runBlockingTest {
        val failingHost = mock<AuthActivityStarter.Host>()
        val ares = requireNotNull(Stripe3ds2AuthResultFixtures.ARES_CHALLENGE_FLOW.ares)
        controller.startChallengeFlow(
            ares,
            transaction,
            SOURCE_ID,
            MAX_TIMEOUT,
            paymentRelayStarter,
            failingHost,
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            REQUEST_OPTIONS
        )

        verify(paymentRelayStarter).start(
            argWhere<PaymentRelayStarter.Args.ErrorArgs> { args ->
                args.exception.message == "Error while attempting to start 3DS2 challenge flow."
            }
        )
    }

    @Test
    fun handlePaymentResult_whenSourceShouldBeCanceled_onlyCallsCancelIntentOnce() = testDispatcher.runBlockingTest {
        // use a PaymentIntent in `requires_action` state
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_3DS1

        val clientSecret = paymentIntent.clientSecret.orEmpty()
        stripeRepository.retrievePaymentIntentResponse = paymentIntent
        stripeRepository.cancelPaymentIntentResponse = paymentIntent

        val sourceId = "src_1Ff87qCRMbs6FrXfPABTYaEd"

        val intent = Intent().putExtras(
            PaymentFlowResult.Unvalidated(
                clientSecret = clientSecret,
                sourceId = sourceId,
                shouldCancelSource = true,
                stripeAccountId = ACCOUNT_ID
            ).toBundle()
        )

        controller.handlePaymentResult(intent, paymentIntentResultCallback)

        assertThat(stripeRepository.retrievePaymentIntentArgs)
            .containsExactly(
                Triple(clientSecret, REQUEST_OPTIONS, listOf("payment_method"))
            )

        // verify that cancelIntent is only called once
        assertThat(stripeRepository.cancelPaymentIntentArgs)
            .containsExactly(
                Triple(paymentIntent.id.orEmpty(), sourceId, REQUEST_OPTIONS)
            )

        verify(paymentIntentResultCallback).onSuccess(
            PaymentIntentResult(paymentIntent)
        )
    }

    @Test
    fun shouldHandleSourceResult_withSourceRequestCode_returnsTrue() {
        assertThat(
            controller.shouldHandleSourceResult(
                StripePaymentController.SOURCE_REQUEST_CODE,
                Intent()
            )
        ).isTrue()
    }

    @Test
    fun handleSourceResult_withSuccessfulResult_shouldCallOnSuccess() {
        controller.handleSourceResult(
            data = Intent().putExtras(
                PaymentFlowResult.Unvalidated(
                    sourceId = "src_123",
                    clientSecret = "src_123_secret_abc"
                ).toBundle()
            ),
            callback = sourceCallback
        )
        verify(sourceCallback).onSuccess(sourceArgumentCaptor.capture())
        assertThat(
            sourceArgumentCaptor.firstValue.status
        ).isEqualTo(Source.Status.Chargeable)

        verifyAnalytics(AnalyticsEvent.AuthSourceResult)
    }

    @Test
    fun startAuthenticateSource_withNoneFlowSource_shouldBypassAuth() {
        controller.startAuthenticateSource(
            host = host,
            source = SourceFixtures.SOURCE_WITH_SOURCE_ORDER.copy(
                flow = Source.Flow.None
            ),
            requestOptions = REQUEST_OPTIONS
        )
        verify(activity).startActivityForResult(
            intentArgumentCaptor.capture(),
            eq(StripePaymentController.SOURCE_REQUEST_CODE)
        )
        val intent = intentArgumentCaptor.firstValue
        assertThat(intent.component?.className)
            .isEqualTo(PaymentRelayActivity::class.java.name)

        verifyAnalytics(AnalyticsEvent.AuthSourceStart)
    }

    @Test
    fun result_creationRoundTrip_shouldReturnExpectedObject() {
        val expectedResult = PaymentFlowResult.Unvalidated(
            clientSecret = "client_secret",
            exception = InvalidRequestException(
                stripeError = StripeErrorFixtures.INVALID_REQUEST_ERROR,
                requestId = "req_123",
                statusCode = 404,
                message = "There was an exception",
                cause = IllegalArgumentException()
            ),
            source = SourceFixtures.CARD,
            sourceId = SourceFixtures.CARD.id,
            flowOutcome = StripeIntentResult.Outcome.SUCCEEDED,
            shouldCancelSource = true
        )
        val resultBundle = ParcelUtils.copy(
            expectedResult.toBundle(),
            Bundle.CREATOR
        )
        assertThat(
            PaymentFlowResult.Unvalidated.fromIntent(
                Intent().putExtras(resultBundle)
            )
        ).isEqualTo(expectedResult)
    }

    @Test
    fun `authenticateAlipay() should return expected outcome`() = testDispatcher.runBlockingTest {
        var actualResult: Result<PaymentIntentResult>? = null
        controller.authenticateAlipay(
            PaymentIntentFixtures.ALIPAY_REQUIRES_ACTION,
            null,
            mock(),
            object : ApiResultCallback<PaymentIntentResult> {
                override fun onSuccess(result: PaymentIntentResult) {
                    actualResult = Result.success(result)
                }

                override fun onError(e: Exception) {
                    actualResult = Result.failure(e)
                }
            }
        )

        val paymentIntentResult = requireNotNull(actualResult?.getOrNull())
        assertThat(paymentIntentResult.outcome)
            .isEqualTo(StripeIntentResult.Outcome.SUCCEEDED)
    }

    @Test
    fun `bypassAuth() with ActivityResultLauncher should use ActivityResultLauncher`() {
        verifyZeroInteractions(activity)

        val launcher = FakeActivityResultLauncher(PaymentRelayContract())
        createController(
            paymentRelayLauncher = launcher
        ).bypassAuth(
            host,
            PaymentIntentFixtures.PI_WITH_LAST_PAYMENT_ERROR,
            null
        )
        assertThat(launcher.launchArgs)
            .containsExactly(
                PaymentRelayStarter.Args.PaymentIntentArgs(
                    PaymentIntentFixtures.PI_WITH_LAST_PAYMENT_ERROR
                )
            )
    }

    @Test
    fun `on3ds2AuthFallback() with ActivityResultLauncher should use ActivityResultLauncher`() {
        verifyZeroInteractions(activity)

        val launcher = FakeActivityResultLauncher(PaymentAuthWebViewContract())
        createController(
            paymentAuthWebViewLauncher = launcher
        ).on3ds2AuthFallback(
            "https://example.com",
            host,
            PaymentIntentFixtures.PI_WITH_LAST_PAYMENT_ERROR,
            REQUEST_OPTIONS
        )
        assertThat(launcher.launchArgs)
            .containsExactly(
                PaymentAuthWebViewContract.Args(
                    requestCode = 50000,
                    clientSecret = "pi_1F7J1aCRMbs6FrXfaJcvbxF6_secret_mIuDLsSfoo1m6s",
                    stripeAccountId = ACCOUNT_ID,
                    url = "https://example.com",
                    shouldCancelSource = true
                )
            )
    }

    private fun createController(
        paymentRelayLauncher: ActivityResultLauncher<PaymentRelayStarter.Args>? = null,
        paymentAuthWebViewLauncher: ActivityResultLauncher<PaymentAuthWebViewContract.Args>? = null
    ): StripePaymentController {
        return StripePaymentController(
            context,
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            stripeRepository,
            false,
            MessageVersionRegistry(),
            CONFIG,
            threeDs2Service,
            analyticsRequestExecutor,
            analyticsDataFactory,
            challengeProgressActivityStarter,
            alipayRepository,
            paymentRelayLauncher = paymentRelayLauncher,
            paymentAuthWebViewLauncher = paymentAuthWebViewLauncher,
            workContext = testDispatcher
        )
    }

    private class FakeStripeRepository : AbsFakeStripeRepository() {
        var retrievePaymentIntentResponse = PaymentIntentFixtures.PI_REQUIRES_REDIRECT
        var cancelPaymentIntentResponse = PaymentIntentFixtures.CANCELLED

        val retrievePaymentIntentArgs = mutableListOf<Triple<String, ApiRequest.Options, List<String>>>()
        val cancelPaymentIntentArgs = mutableListOf<Triple<String, String, ApiRequest.Options>>()

        override suspend fun retrieveSetupIntent(
            clientSecret: String,
            options: ApiRequest.Options,
            expandFields: List<String>
        ) = SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT

        override suspend fun retrievePaymentIntent(
            clientSecret: String,
            options: ApiRequest.Options,
            expandFields: List<String>
        ): PaymentIntent {
            retrievePaymentIntentArgs.add(
                Triple(clientSecret, options, expandFields)
            )
            return retrievePaymentIntentResponse
        }

        override suspend fun retrieveSource(
            sourceId: String,
            clientSecret: String,
            options: ApiRequest.Options
        ) = SourceFixtures.SOURCE_CARD.copy(status = Source.Status.Chargeable)

        override suspend fun cancelPaymentIntentSource(
            paymentIntentId: String,
            sourceId: String,
            options: ApiRequest.Options
        ): PaymentIntent {
            cancelPaymentIntentArgs.add(
                Triple(paymentIntentId, sourceId, options)
            )
            return cancelPaymentIntentResponse
        }

        override suspend fun cancelSetupIntentSource(
            setupIntentId: String,
            sourceId: String,
            options: ApiRequest.Options
        ) = SetupIntentFixtures.CANCELLED
    }

    private fun verifyAnalytics(event: AnalyticsEvent) {
        verify(analyticsRequestExecutor)
            .executeAsync(analyticsRequestArgumentCaptor.capture())
        val analyticsRequest = analyticsRequestArgumentCaptor.firstValue
        assertThat(
            analyticsRequest.compactParams?.get(AnalyticsDataFactory.FIELD_EVENT)
        ).isEqualTo(event.toString())
    }

    private class FakeAlipayRepostiory : AlipayRepository {
        override suspend fun authenticate(
            intent: StripeIntent,
            authenticator: AlipayAuthenticator,
            requestOptions: ApiRequest.Options
        ) = AlipayAuthResult(StripeIntentResult.Outcome.SUCCEEDED)
    }

    private companion object {
        private const val ACCOUNT_ID = "acct_123"
        private const val MESSAGE_VERSION = Stripe3ds2Fixtures.MESSAGE_VERSION
        private val REQUEST_OPTIONS = ApiRequest.Options(
            apiKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            stripeAccount = ACCOUNT_ID
        )
        private const val MAX_TIMEOUT = 5
        private const val SOURCE_ID = "src_123"

        private val CONFIG = PaymentAuthConfig.Builder()
            .set3ds2Config(
                PaymentAuthConfig.Stripe3ds2Config.Builder()
                    .setTimeout(5)
                    .build()
            )
            .build()

        private const val MASTERCARD_DS_ID = "A000000004"
        private const val AMEX_DS_ID = "A000000025"
    }
}
