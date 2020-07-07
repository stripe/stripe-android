package com.stripe.android

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import com.stripe.android.exception.APIException
import com.stripe.android.exception.InvalidRequestException
import com.stripe.android.model.Complete3ds2Result
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.Source
import com.stripe.android.model.SourceFixtures
import com.stripe.android.model.Stripe3ds2AuthParams
import com.stripe.android.model.Stripe3ds2AuthResult
import com.stripe.android.model.Stripe3ds2AuthResultFixtures
import com.stripe.android.model.Stripe3ds2Fingerprint
import com.stripe.android.model.Stripe3ds2FingerprintTest
import com.stripe.android.model.StripeIntent
import com.stripe.android.stripe3ds2.service.StripeThreeDs2Service
import com.stripe.android.stripe3ds2.transaction.CompletionEvent
import com.stripe.android.stripe3ds2.transaction.ErrorMessage
import com.stripe.android.stripe3ds2.transaction.MessageVersionRegistry
import com.stripe.android.stripe3ds2.transaction.ProtocolErrorEvent
import com.stripe.android.stripe3ds2.transaction.RuntimeErrorEvent
import com.stripe.android.stripe3ds2.transaction.Transaction
import com.stripe.android.utils.ParcelUtils
import com.stripe.android.view.AuthActivityStarter
import com.stripe.android.view.PaymentRelayActivity
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
class StripePaymentControllerTest {

    private val activity: Activity = mock()
    private val threeDs2Service: StripeThreeDs2Service = mock()
    private val transaction: Transaction = mock()
    private val paymentAuthResultCallback: ApiResultCallback<PaymentIntentResult> = mock()
    private val setupAuthResultCallback: ApiResultCallback<SetupIntentResult> = mock()
    private val sourceCallback: ApiResultCallback<Source> = mock()
    private val paymentRelayStarter: PaymentRelayStarter = mock()
    private val analyticsRequestExecutor: AnalyticsRequestExecutor = mock()
    private val challengeFlowStarter: StripePaymentController.ChallengeFlowStarter = mock()
    private val challengeProgressActivityStarter: StripePaymentController.ChallengeProgressActivityStarter = mock()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val controller: PaymentController by lazy {
        createController()
    }
    private val analyticsDataFactory = AnalyticsDataFactory(
        context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
    private val host = AuthActivityStarter.Host.create(activity)

    private val relayStarterArgsArgumentCaptor: KArgumentCaptor<PaymentRelayStarter.Args> = argumentCaptor()
    private val intentArgumentCaptor: KArgumentCaptor<Intent> = argumentCaptor()
    private val analyticsRequestArgumentCaptor: KArgumentCaptor<AnalyticsRequest> = argumentCaptor()
    private val setupIntentResultArgumentCaptor: KArgumentCaptor<SetupIntentResult> = argumentCaptor()
    private val apiResultStripeIntentArgumentCaptor: KArgumentCaptor<ApiResultCallback<StripeIntent>> = argumentCaptor()
    private val sourceArgumentCaptor: KArgumentCaptor<Source> = argumentCaptor()

    private val testScope = TestCoroutineScope(TestCoroutineDispatcher())

    @BeforeTest
    fun setup() {
        whenever(transaction.authenticationRequestParameters)
            .thenReturn(Stripe3ds2Fixtures.AREQ_PARAMS)
        whenever(activity.applicationContext)
            .thenReturn(context)
    }

    @Test
    fun handleNextAction_withMastercardAnd3ds2_shouldStart3ds2ChallengeFlow() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2
        val dsPublicKey = Stripe3ds2Fingerprint(paymentIntent.nextActionData as StripeIntent.NextActionData.SdkData.Use3DS2)
            .directoryServerEncryption
            .directoryServerPublicKey
        whenever(
            threeDs2Service.createTransaction(
                eq(Stripe3ds2Fingerprint.DirectoryServer.Mastercard.id),
                eq(MESSAGE_VERSION),
                eq(paymentIntent.isLiveMode),
                eq(Stripe3ds2Fingerprint.DirectoryServer.Mastercard.networkName),
                any(),
                eq(dsPublicKey),
                eq("7c4debe3f4af7f9d1569a2ffea4343c2566826ee"),
                any(),
                any()
            )
        )
            .thenReturn(transaction)
        controller.handleNextAction(host, paymentIntent, REQUEST_OPTIONS)
        verify(threeDs2Service).createTransaction(
            eq(Stripe3ds2Fingerprint.DirectoryServer.Mastercard.id),
            eq(MESSAGE_VERSION),
            eq(paymentIntent.isLiveMode),
            eq(Stripe3ds2Fingerprint.DirectoryServer.Mastercard.networkName),
            any(),
            eq(dsPublicKey),
            eq("7c4debe3f4af7f9d1569a2ffea4343c2566826ee"),
            any(),
            any()
        )
        verify(challengeFlowStarter)
            .start(any())

        verify(challengeProgressActivityStarter).start(
            eq(activity),
            eq("mastercard"),
            eq(false),
            any()
        )

        verify(analyticsRequestExecutor)
            .executeAsync(analyticsRequestArgumentCaptor.capture())
        val analyticsParams = requireNotNull(analyticsRequestArgumentCaptor.firstValue.params)
        assertEquals(
            AnalyticsEvent.Auth3ds2Fingerprint.toString(),
            analyticsParams[AnalyticsDataFactory.FIELD_EVENT]
        )
        assertEquals(
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2.id,
            analyticsParams[AnalyticsDataFactory.FIELD_INTENT_ID]
        )
    }

    @Test
    fun handleNextAction_withAmexAnd3ds2_shouldStart3ds2ChallengeFlow() {
        whenever(threeDs2Service.createTransaction(
            eq(Stripe3ds2Fingerprint.DirectoryServer.Amex.id),
            eq(MESSAGE_VERSION),
            eq(PaymentIntentFixtures.PI_REQUIRES_AMEX_3DS2.isLiveMode),
            eq(Stripe3ds2Fingerprint.DirectoryServer.Amex.networkName),
            any(),
            eq(Stripe3ds2FingerprintTest.DS_RSA_PUBLIC_KEY),
            eq(PaymentIntentFixtures.KEY_ID),
            any(),
            any()
        ))
            .thenReturn(transaction)
        controller.handleNextAction(
            host,
            PaymentIntentFixtures.PI_REQUIRES_AMEX_3DS2,
            REQUEST_OPTIONS
        )
        verify(threeDs2Service).createTransaction(
            eq(Stripe3ds2Fingerprint.DirectoryServer.Amex.id),
            eq(MESSAGE_VERSION),
            eq(PaymentIntentFixtures.PI_REQUIRES_AMEX_3DS2.isLiveMode),
            eq(Stripe3ds2Fingerprint.DirectoryServer.Amex.networkName),
            any(),
            eq(Stripe3ds2FingerprintTest.DS_RSA_PUBLIC_KEY),
            eq(PaymentIntentFixtures.KEY_ID),
            any(),
            any()
        )
        verify(challengeFlowStarter)
            .start(any())

        verify(challengeProgressActivityStarter).start(
            eq(activity),
            eq("american_express"),
            eq(false),
            any()
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
        val args: PaymentAuthWebViewStarter.Args = requireNotNull(
            intentArgumentCaptor.firstValue.getParcelableExtra(PaymentAuthWebViewStarter.EXTRA_ARGS)
        )
        assertEquals(
            "https://hooks.stripe.com/3d_secure_2_eap/begin_test/src_1Ecve7CRMbs6FrXfm8AxXMIh/src_client_secret_F79yszOBAiuaZTuIhbn3LPUW",
            args.url
        )
        assertNull(args.returnUrl)

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
        val args: PaymentAuthWebViewStarter.Args = requireNotNull(
            intentArgumentCaptor.firstValue.getParcelableExtra(PaymentAuthWebViewStarter.EXTRA_ARGS)
        )
        assertEquals(
            "https://hooks.stripe.com/3d_secure_2_eap/begin_test/src_1Ecaz6CRMbs6FrXfuYKBRSUG/src_client_secret_F6octeOshkgxT47dr0ZxSZiv",
            args.url
        )
        assertEquals("stripe://deeplink", args.returnUrl)

        verify(analyticsRequestExecutor)
            .executeAsync(analyticsRequestArgumentCaptor.capture())
        val analyticsParams = requireNotNull(analyticsRequestArgumentCaptor.firstValue.params)
        assertEquals(
            AnalyticsEvent.AuthRedirect.toString(),
            analyticsParams[AnalyticsDataFactory.FIELD_EVENT]
        )
        assertEquals("pi_1EZlvVCRMbs6FrXfKpq2xMmy",
            analyticsParams[AnalyticsDataFactory.FIELD_INTENT_ID])
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
    fun shouldHandleResult_withInvalidResultCode() {
        assertFalse(controller.shouldHandlePaymentResult(500, Intent()))
        assertFalse(controller.shouldHandleSetupResult(500, Intent()))
    }

    @Test
    fun getRequestCode_withIntents_correctCodeReturned() {
        assertEquals(
            StripePaymentController.PAYMENT_REQUEST_CODE,
            StripePaymentController.getRequestCode(PaymentIntentFixtures.PI_REQUIRES_3DS1)
        )
        assertEquals(
            StripePaymentController.SETUP_REQUEST_CODE,
            StripePaymentController.getRequestCode(SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT)
        )
    }

    @Test
    fun getRequestCode_withParams_correctCodeReturned() {
        assertEquals(StripePaymentController.PAYMENT_REQUEST_CODE,
            StripePaymentController.getRequestCode(
                ConfirmPaymentIntentParams.createWithPaymentMethodId(
                    "pm_123", "client_secret", ""
                )
            )
        )
    }

    @Test
    fun test3ds2Receiver_whenCompleted_shouldFireAnalyticsRequest() {
        val completionEvent = CompletionEvent(
            sdkTransactionId = "8dd3413f-0b45-4234-bc45-6cc40fb1b0f1",
            transactionStatus = "C"
        )

        whenever(transaction.initialChallengeUiType).thenReturn("04")

        val receiver = StripePaymentController.PaymentAuth3ds2ChallengeStatusReceiver(
            FakeStripeRepository(),
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            "src_123",
            REQUEST_OPTIONS,
            analyticsRequestExecutor,
            analyticsDataFactory,
            transaction,
            AnalyticsRequest.Factory()
        )
        receiver.completed(completionEvent, "01", onReceiverCompleted = {})

        verify(analyticsRequestExecutor, times(2))
            .executeAsync(analyticsRequestArgumentCaptor.capture())
        val analyticsRequests = analyticsRequestArgumentCaptor.allValues

        assertEquals(
            AnalyticsEvent.Auth3ds2ChallengeCompleted.toString(),
            requireNotNull(analyticsRequests[0].params)[AnalyticsDataFactory.FIELD_EVENT]
        )

        val analyticsParamsSecond = requireNotNull(analyticsRequests[1].params)
        assertEquals(
            AnalyticsEvent.Auth3ds2ChallengePresented.toString(),
            analyticsParamsSecond[AnalyticsDataFactory.FIELD_EVENT]
        )
        assertEquals(
            "oob",
            analyticsParamsSecond[AnalyticsDataFactory.FIELD_3DS2_UI_TYPE]
        )
    }

    @Test
    fun test3ds2Receiver_whenTimedout_shouldFireAnalyticsRequest() {
        val receiver = StripePaymentController.PaymentAuth3ds2ChallengeStatusReceiver(
            FakeStripeRepository(),
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            "src_123",
            ApiRequest.Options(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            analyticsRequestExecutor,
            analyticsDataFactory,
            transaction,
            AnalyticsRequest.Factory()
        )
        receiver.timedout("01", onReceiverCompleted = {})
        verify(analyticsRequestExecutor, times(2))
            .executeAsync(analyticsRequestArgumentCaptor.capture())
        val analyticsRequests = analyticsRequestArgumentCaptor.allValues

        assertEquals(
            AnalyticsEvent.Auth3ds2ChallengeTimedOut.toString(),
            requireNotNull(analyticsRequests[0].params)[AnalyticsDataFactory.FIELD_EVENT]
        )

        assertEquals(
            AnalyticsEvent.Auth3ds2ChallengePresented.toString(),
            requireNotNull(analyticsRequests[1].params)[AnalyticsDataFactory.FIELD_EVENT]
        )
    }

    @Test
    fun test3ds2Receiver_whenCanceled_shouldFireAnalyticsRequest() {
        val receiver = StripePaymentController.PaymentAuth3ds2ChallengeStatusReceiver(
            FakeStripeRepository(),
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            "src_123",
            ApiRequest.Options(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            analyticsRequestExecutor,
            analyticsDataFactory,
            transaction,
            AnalyticsRequest.Factory()
        )
        receiver.cancelled("01", onReceiverCompleted = {})

        verify(analyticsRequestExecutor, times(2))
            .executeAsync(analyticsRequestArgumentCaptor.capture())
        val analyticsRequests = analyticsRequestArgumentCaptor.allValues

        assertEquals(
            AnalyticsEvent.Auth3ds2ChallengeCanceled.toString(),
            requireNotNull(analyticsRequests[0].params)[AnalyticsDataFactory.FIELD_EVENT]
        )

        assertEquals(
            AnalyticsEvent.Auth3ds2ChallengePresented.toString(),
            requireNotNull(analyticsRequests[1].params)[AnalyticsDataFactory.FIELD_EVENT]
        )
    }

    @Test
    fun test3ds2Receiver_whenRuntimeErrorError_shouldFireAnalyticsRequest() {
        val runtimeErrorEvent = RuntimeErrorEvent(
            errorCode = "404",
            errorMessage = "Resource not found"
        )

        val receiver = StripePaymentController.PaymentAuth3ds2ChallengeStatusReceiver(
            FakeStripeRepository(),
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            "src_123",
            ApiRequest.Options(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            analyticsRequestExecutor,
            analyticsDataFactory,
            transaction,
            AnalyticsRequest.Factory()
        )
        receiver.runtimeError(runtimeErrorEvent, onReceiverCompleted = {})

        verify(analyticsRequestExecutor, times(2))
            .executeAsync(analyticsRequestArgumentCaptor.capture())
        val analyticsRequests = analyticsRequestArgumentCaptor.allValues

        val analyticsParamsFirst = requireNotNull(analyticsRequests[0].params)
        assertEquals(
            AnalyticsEvent.Auth3ds2ChallengeErrored.toString(),
            analyticsParamsFirst[AnalyticsDataFactory.FIELD_EVENT]
        )

        assertThat(analyticsParamsFirst[AnalyticsDataFactory.FIELD_ERROR_DATA])
            .isEqualTo(
                mapOf(
                    "type" to "runtime_error_event",
                    "error_code" to "404",
                    "error_message" to "Resource not found"
                )
            )

        assertEquals(
            AnalyticsEvent.Auth3ds2ChallengePresented.toString(),
            requireNotNull(analyticsRequests[1].params)[AnalyticsDataFactory.FIELD_EVENT]
        )
    }

    @Test
    fun test3ds2Receiver_whenProtocolError_shouldFireAnalyticsRequest() {
        val protocolErrorEvent = ProtocolErrorEvent(
            sdkTransactionId = "8dd3413f-0b45-4234-bc45-6cc40fb1b0f1",
            errorMessage = ErrorMessage(
                errorCode = "201",
                errorDescription = "Required element missing",
                errorDetails = "eci",
                transactionId = "047f76a6-d1d4-48a2-aa65-786abb6f7f46"
            )
        )

        val receiver = StripePaymentController.PaymentAuth3ds2ChallengeStatusReceiver(
            FakeStripeRepository(),
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            "src_123",
            ApiRequest.Options(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            analyticsRequestExecutor,
            analyticsDataFactory,
            transaction,
            AnalyticsRequest.Factory()
        )
        receiver.protocolError(protocolErrorEvent, onReceiverCompleted = {})

        verify(analyticsRequestExecutor, times(2))
            .executeAsync(analyticsRequestArgumentCaptor.capture())
        val analyticsRequests = analyticsRequestArgumentCaptor.allValues

        val analyticsParamsFirst = requireNotNull(analyticsRequests[0].params)
        assertEquals(
            AnalyticsEvent.Auth3ds2ChallengeErrored.toString(),
            analyticsParamsFirst[AnalyticsDataFactory.FIELD_EVENT]
        )

        assertThat(analyticsParamsFirst[AnalyticsDataFactory.FIELD_ERROR_DATA])
            .isEqualTo(
                mapOf(
                    "type" to "protocol_error_event",
                    "error_code" to "201",
                    "sdk_trans_id" to "8dd3413f-0b45-4234-bc45-6cc40fb1b0f1",
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
        var onReceiverCompletedCalls = 0

        val receiver = StripePaymentController.PaymentAuth3ds2ChallengeStatusReceiver(
            FakeStripeRepository(),
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            "src_123",
            ApiRequest.Options(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            analyticsRequestExecutor,
            analyticsDataFactory,
            transaction,
            AnalyticsRequest.Factory()
        )
        receiver.cancelled("01", onReceiverCompleted = {
            onReceiverCompletedCalls++
        })

        assertThat(onReceiverCompletedCalls)
            .isEqualTo(1)
    }

    @Test
    fun getClientSecret_shouldGetClientSecretFromIntent() {
        val data = Intent().putExtras(
            PaymentController.Result(
                clientSecret = PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2.clientSecret
            ).toBundle()
        )
        assertNotNull(PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2.clientSecret)
        assertEquals(PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2.clientSecret,
            StripePaymentController.getClientSecret(data))
    }

    @Test
    fun handlePaymentResult_withAuthException_shouldCallCallbackOnError() {
        val exception = APIException(RuntimeException())
        val intent = Intent().putExtras(
            PaymentController.Result(
                exception = exception
            ).toBundle()
        )

        controller.handlePaymentResult(intent, paymentAuthResultCallback)
        verify(paymentAuthResultCallback).onError(exception)
        verify(paymentAuthResultCallback, never())
            .onSuccess(anyOrNull())
    }

    @Test
    fun handleSetupResult_withAuthException_shouldCallCallbackOnError() {
        val exception = APIException(RuntimeException())
        val intent = Intent().putExtras(
            PaymentController.Result(
                exception = exception
            ).toBundle()
        )

        controller.handleSetupResult(intent, setupAuthResultCallback)

        verify(setupAuthResultCallback).onError(exception)
        verify(setupAuthResultCallback, never())
            .onSuccess(anyOrNull())
    }

    @Test
    fun handleSetupResult_shouldCallbackOnSuccess() {
        assertNotNull(SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT.clientSecret)

        val intent = Intent().putExtras(
            PaymentController.Result(
                clientSecret = SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT.clientSecret,
                flowOutcome = StripeIntentResult.Outcome.SUCCEEDED
            ).toBundle()
        )

        controller.handleSetupResult(intent, setupAuthResultCallback)

        verify(setupAuthResultCallback).onSuccess(setupIntentResultArgumentCaptor.capture())
        val result = setupIntentResultArgumentCaptor.firstValue
        assertEquals(StripeIntentResult.Outcome.SUCCEEDED, result.outcome)
        assertEquals(SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT, result.intent)
    }

    @Test
    fun authCallback_withChallengeFlow_shouldNotStartRelayActivity() {
        val authCallback = StripePaymentController.Stripe3ds2AuthCallback(
            host, FakeStripeRepository(), transaction, MAX_TIMEOUT,
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2, SOURCE_ID,
            REQUEST_OPTIONS, analyticsRequestExecutor, analyticsDataFactory,
            challengeFlowStarter,
            paymentRelayStarter = paymentRelayStarter
        )
        authCallback.onSuccess(Stripe3ds2AuthResultFixtures.ARES_CHALLENGE_FLOW)
        verify(paymentRelayStarter, never())
            .start(anyOrNull())
    }

    @Test
    fun authCallback_withFrictionlessFlow_shouldStartRelayActivityWithPaymentIntent() {
        val authCallback = StripePaymentController.Stripe3ds2AuthCallback(
            host, FakeStripeRepository(), transaction, MAX_TIMEOUT,
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2, SOURCE_ID,
            REQUEST_OPTIONS, analyticsRequestExecutor, analyticsDataFactory,
            challengeFlowStarter,
            paymentRelayStarter = paymentRelayStarter
        )
        authCallback.onSuccess(Stripe3ds2AuthResultFixtures.ARES_FRICTIONLESS_FLOW)
        verify(paymentRelayStarter)
            .start(relayStarterArgsArgumentCaptor.capture())
        assertEquals(PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            relayStarterArgsArgumentCaptor.firstValue.stripeIntent)

        verify(analyticsRequestExecutor)
            .executeAsync(analyticsRequestArgumentCaptor.capture())
        val analyticsRequest = analyticsRequestArgumentCaptor.firstValue
        val analyticsParams = requireNotNull(analyticsRequest.params)
        assertEquals(
            AnalyticsEvent.Auth3ds2Frictionless.toString(),
            analyticsParams[AnalyticsDataFactory.FIELD_EVENT]
        )
        assertEquals("pi_1ExkUeAWhjPjYwPiXph9ouXa",
            analyticsParams[AnalyticsDataFactory.FIELD_INTENT_ID])
    }

    @Test
    fun authCallback_withFallbackRedirectUrl_shouldStartAuthWebView() {
        val authCallback = StripePaymentController.Stripe3ds2AuthCallback(
            host, FakeStripeRepository(), transaction, MAX_TIMEOUT,
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2, SOURCE_ID,
            REQUEST_OPTIONS, analyticsRequestExecutor, analyticsDataFactory,
            challengeFlowStarter,
            paymentRelayStarter = paymentRelayStarter
        )
        authCallback.onSuccess(Stripe3ds2AuthResultFixtures.FALLBACK_REDIRECT_URL)
        verify(activity).startActivityForResult(intentArgumentCaptor.capture(),
            eq(StripePaymentController.PAYMENT_REQUEST_CODE))
        val args: PaymentAuthWebViewStarter.Args = requireNotNull(
            intentArgumentCaptor.firstValue.getParcelableExtra(PaymentAuthWebViewStarter.EXTRA_ARGS)
        )
        assertEquals(
            "https://hooks.stripe.com/3d_secure_2_eap/begin_test/src_1Ecve7CRMbs6FrXfm8AxXMIh/src_client_secret_F79yszOBAiuaZTuIhbn3LPUW",
            args.url
        )
        assertNull(args.returnUrl)

        verify(analyticsRequestExecutor)
            .executeAsync(analyticsRequestArgumentCaptor.capture())
        val analyticsRequest = analyticsRequestArgumentCaptor.firstValue
        assertEquals(
            AnalyticsEvent.Auth3ds2Fallback.toString(),
            requireNotNull(analyticsRequest.params)[AnalyticsDataFactory.FIELD_EVENT]
        )
    }

    @Test
    fun authCallback_withError_shouldStartRelayActivityWithException() {
        val authCallback = StripePaymentController.Stripe3ds2AuthCallback(
            host, FakeStripeRepository(), transaction, MAX_TIMEOUT,
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2, SOURCE_ID,
            REQUEST_OPTIONS, analyticsRequestExecutor, analyticsDataFactory,
            challengeFlowStarter,
            paymentRelayStarter = paymentRelayStarter
        )
        authCallback.onSuccess(Stripe3ds2AuthResultFixtures.ERROR)
        verify(paymentRelayStarter).start(relayStarterArgsArgumentCaptor.capture())
        assertEquals("Error encountered during 3DS2 authentication request. " +
            "Code: 302, Detail: null, " +
            "Description: Data could not be decrypted by the receiving system due to " +
            "technical or other reason., Component: D",
            relayStarterArgsArgumentCaptor.firstValue.exception?.message)
    }

    @Test
    fun handlePaymentResult_whenSourceShouldBeCanceled_onlyCallsCancelIntentOnce() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_3DS1
        val clientSecret = paymentIntent.clientSecret.orEmpty()
        val stripeRepository: StripeRepository = mock()
        val sourceId = "src_1Ff87qCRMbs6FrXfPABTYaEd"

        val intent = Intent().putExtras(
            PaymentController.Result(
                clientSecret = clientSecret,
                sourceId = sourceId,
                shouldCancelSource = true
            ).toBundle()
        )

        createController(stripeRepository)
            .handlePaymentResult(intent, paymentAuthResultCallback)

        verify(stripeRepository).retrieveIntent(
            eq(clientSecret),
            eq(REQUEST_OPTIONS),
            eq(listOf("payment_method")),
            apiResultStripeIntentArgumentCaptor.capture()
        )
        // return a PaymentIntent in `requires_action` state
        apiResultStripeIntentArgumentCaptor.firstValue.onSuccess(paymentIntent)

        verify(stripeRepository).cancelIntent(
            eq(paymentIntent),
            eq(sourceId),
            eq(REQUEST_OPTIONS),
            apiResultStripeIntentArgumentCaptor.capture()
        )
        apiResultStripeIntentArgumentCaptor.secondValue.onSuccess(paymentIntent)

        // verify that cancelIntent is only called once
        verifyNoMoreInteractions(stripeRepository)

        verify(paymentAuthResultCallback).onSuccess(
            PaymentIntentResult(paymentIntent)
        )
    }

    @Test
    fun shouldHandleSourceResult_withSourceRequestCode_returnsTrue() {
        assertTrue(
            controller.shouldHandleSourceResult(StripePaymentController.SOURCE_REQUEST_CODE, Intent())
        )
    }

    @Test
    fun handleSourceResult_withSuccessfulResult_shouldCallOnSuccess() {
        controller.handleSourceResult(
            data = Intent().putExtras(
                PaymentController.Result(
                    sourceId = "src_123",
                    clientSecret = "src_123_secret_abc"
                ).toBundle()
            ),
            callback = sourceCallback
        )
        verify(sourceCallback).onSuccess(sourceArgumentCaptor.capture())
        assertEquals(
            Source.Status.Chargeable,
            sourceArgumentCaptor.firstValue.status
        )

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
        val expectedResult = PaymentController.Result(
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
        val actualResult =
            PaymentController.Result.fromIntent(Intent().putExtras(resultBundle))
        assertEquals(expectedResult, actualResult)
    }

    private fun createController(
        stripeRepository: StripeRepository = FakeStripeRepository()
    ): PaymentController {
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
            challengeFlowStarter,
            challengeProgressActivityStarter,
            testScope
        )
    }

    private class FakeStripeRepository : AbsFakeStripeRepository() {
        override fun retrieveSetupIntent(
            clientSecret: String,
            options: ApiRequest.Options,
            expandFields: List<String>
        ): SetupIntent {
            return SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT
        }

        override fun start3ds2Auth(
            authParams: Stripe3ds2AuthParams,
            stripeIntentId: String,
            requestOptions: ApiRequest.Options,
            callback: ApiResultCallback<Stripe3ds2AuthResult>
        ) {
            callback.onSuccess(Stripe3ds2AuthResultFixtures.ARES_CHALLENGE_FLOW)
        }

        override fun complete3ds2Auth(
            sourceId: String,
            requestOptions: ApiRequest.Options,
            callback: ApiResultCallback<Complete3ds2Result>
        ) {
            callback.onSuccess(Complete3ds2Result(true))
        }

        override fun retrieveIntent(
            clientSecret: String,
            options: ApiRequest.Options,
            expandFields: List<String>,
            callback: ApiResultCallback<StripeIntent>
        ) {
            super.retrieveIntent(clientSecret, options, expandFields, callback)
            callback.onSuccess(SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT)
        }

        override fun retrieveSource(
            sourceId: String,
            clientSecret: String,
            options: ApiRequest.Options,
            callback: ApiResultCallback<Source>
        ) {
            callback.onSuccess(
                SourceFixtures.SOURCE_CARD.copy(status = Source.Status.Chargeable)
            )
        }
    }

    private fun verifyAnalytics(event: AnalyticsEvent) {
        verify(analyticsRequestExecutor)
            .executeAsync(analyticsRequestArgumentCaptor.capture())
        val analyticsRequest = analyticsRequestArgumentCaptor.firstValue
        assertEquals(
            event.toString(),
            analyticsRequest.compactParams?.get(AnalyticsDataFactory.FIELD_EVENT)
        )
    }

    private companion object {
        private const val MESSAGE_VERSION = Stripe3ds2Fixtures.MESSAGE_VERSION
        private val REQUEST_OPTIONS =
            ApiRequest.Options(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        private const val MAX_TIMEOUT = 5
        private const val SOURCE_ID = "src_123"

        private val CONFIG = PaymentAuthConfig.Builder()
            .set3ds2Config(PaymentAuthConfig.Stripe3ds2Config.Builder()
                .setTimeout(5)
                .build())
            .build()
    }
}
