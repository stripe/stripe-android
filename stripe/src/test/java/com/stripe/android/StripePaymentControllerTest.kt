package com.stripe.android

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.Stripe3ds2AuthResult
import com.stripe.android.model.Stripe3ds2AuthResultFixtures
import com.stripe.android.model.Stripe3ds2Fingerprint
import com.stripe.android.model.Stripe3ds2FingerprintTest
import com.stripe.android.model.StripeIntent
import com.stripe.android.stripe3ds2.service.StripeThreeDs2Service
import com.stripe.android.stripe3ds2.transaction.AuthenticationRequestParameters
import com.stripe.android.stripe3ds2.transaction.CompletionEvent
import com.stripe.android.stripe3ds2.transaction.ErrorMessage
import com.stripe.android.stripe3ds2.transaction.MessageVersionRegistry
import com.stripe.android.stripe3ds2.transaction.ProtocolErrorEvent
import com.stripe.android.stripe3ds2.transaction.RuntimeErrorEvent
import com.stripe.android.stripe3ds2.transaction.Transaction
import com.stripe.android.stripe3ds2.views.ChallengeProgressDialogActivity
import com.stripe.android.view.AuthActivityStarter
import com.stripe.android.view.StripeIntentResultExtras
import java.security.cert.CertificateException
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.MainScope
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StripePaymentControllerTest {

    private lateinit var controller: StripePaymentController
    private lateinit var analyticsDataFactory: AnalyticsDataFactory
    private lateinit var host: AuthActivityStarter.Host

    @Mock
    private lateinit var activity: Activity
    @Mock
    private lateinit var threeDs2Service: StripeThreeDs2Service
    @Mock
    private lateinit var transaction: Transaction
    @Mock
    private lateinit var paymentAuthResultCallback: ApiResultCallback<PaymentIntentResult>
    @Mock
    private lateinit var setupAuthResultCallback: ApiResultCallback<SetupIntentResult>
    @Mock
    private lateinit var complete3ds2AuthCallback: ApiResultCallback<Boolean>
    @Mock
    private lateinit var paymentRelayStarter: PaymentRelayStarter
    @Mock
    private lateinit var fireAndForgetRequestExecutor: FireAndForgetRequestExecutor
    @Mock
    private lateinit var challengeFlowStarter: StripePaymentController.ChallengeFlowStarter

    private val complete3ds2AuthCallbackFactory =
        object : StripePaymentController.PaymentAuth3ds2ChallengeStatusReceiver.Complete3ds2AuthCallbackFactory {
            override fun create(
                arg: Stripe3ds2CompletionStarter.Args
            ): ApiResultCallback<Boolean> {
                return complete3ds2AuthCallback
            }
        }

    private lateinit var relayStarterArgsArgumentCaptor: KArgumentCaptor<PaymentRelayStarter.Args>
    private lateinit var intentArgumentCaptor: KArgumentCaptor<Intent>
    private lateinit var apiRequestArgumentCaptor: KArgumentCaptor<StripeRequest>
    private lateinit var resultCaptor: KArgumentCaptor<SetupIntentResult>

    @BeforeTest
    fun setup() {
        MockitoAnnotations.initMocks(this)

        relayStarterArgsArgumentCaptor = argumentCaptor()
        intentArgumentCaptor = argumentCaptor()
        apiRequestArgumentCaptor = argumentCaptor()
        resultCaptor = argumentCaptor()

        host = AuthActivityStarter.Host.create(activity)
        analyticsDataFactory =
            AnalyticsDataFactory.create(ApplicationProvider.getApplicationContext<Context>())
        `when`<AuthenticationRequestParameters>(transaction.authenticationRequestParameters)
            .thenReturn(Stripe3ds2Fixtures.AREQ_PARAMS)
        `when`<Context>(activity.applicationContext)
            .thenReturn(ApplicationProvider.getApplicationContext<Context>())
        controller = StripePaymentController(
            ApplicationProvider.getApplicationContext<Context>(),
            FakeStripeRepository(),
            false,
            null,
            MessageVersionRegistry(),
            CONFIG,
            threeDs2Service,
            fireAndForgetRequestExecutor,
            analyticsDataFactory,
            challengeFlowStarter,
            MainScope()
        )
    }

    @Test
    @Throws(CertificateException::class)
    fun handleNextAction_withMastercardAnd3ds2_shouldStart3ds2ChallengeFlow() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2
        val dsPublicKey = Stripe3ds2Fingerprint.create(
            requireNotNull(paymentIntent.stripeSdkData)
        )
            .directoryServerEncryption
            .directoryServerPublicKey
        `when`(threeDs2Service.createTransaction(
            eq(Stripe3ds2Fingerprint.DirectoryServer.Mastercard.id),
            eq(MESSAGE_VERSION),
            eq(paymentIntent.isLiveMode),
            eq(Stripe3ds2Fingerprint.DirectoryServer.Mastercard.networkName),
            any(),
            eq(dsPublicKey),
            eq("7c4debe3f4af7f9d1569a2ffea4343c2566826ee")))
            .thenReturn(transaction)
        controller.handleNextAction(host, paymentIntent, REQUEST_OPTIONS)
        verify(threeDs2Service).createTransaction(
            eq(Stripe3ds2Fingerprint.DirectoryServer.Mastercard.id),
            eq(MESSAGE_VERSION),
            eq(paymentIntent.isLiveMode),
            eq(Stripe3ds2Fingerprint.DirectoryServer.Mastercard.networkName),
            any(),
            eq(dsPublicKey),
            eq("7c4debe3f4af7f9d1569a2ffea4343c2566826ee"))
        verify(challengeFlowStarter)
            .start(any())

        verify(activity).startActivity(eq(
            Intent(activity, ChallengeProgressDialogActivity::class.java)))

        verify(fireAndForgetRequestExecutor)
            .executeAsync(apiRequestArgumentCaptor.capture())
        val analyticsParams = requireNotNull(apiRequestArgumentCaptor.firstValue.params)
        assertEquals("stripe_android.3ds2_fingerprint",
            analyticsParams[AnalyticsDataFactory.FIELD_EVENT])
        assertEquals(PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2.id,
            analyticsParams[AnalyticsDataFactory.FIELD_INTENT_ID])
    }

    @Test
    fun handleNextAction_withAmexAnd3ds2_shouldStart3ds2ChallengeFlow() {
        `when`(threeDs2Service.createTransaction(
            eq(Stripe3ds2Fingerprint.DirectoryServer.Amex.id),
            eq(MESSAGE_VERSION),
            eq(PaymentIntentFixtures.PI_REQUIRES_AMEX_3DS2.isLiveMode),
            eq(Stripe3ds2Fingerprint.DirectoryServer.Amex.networkName),
            any(),
            eq(Stripe3ds2FingerprintTest.DS_RSA_PUBLIC_KEY),
            eq(PaymentIntentFixtures.KEY_ID)))
            .thenReturn(transaction)
        controller.handleNextAction(host, PaymentIntentFixtures.PI_REQUIRES_AMEX_3DS2,
            REQUEST_OPTIONS)
        verify(threeDs2Service).createTransaction(
            eq(Stripe3ds2Fingerprint.DirectoryServer.Amex.id),
            eq(MESSAGE_VERSION),
            eq(PaymentIntentFixtures.PI_REQUIRES_AMEX_3DS2.isLiveMode),
            eq(Stripe3ds2Fingerprint.DirectoryServer.Amex.networkName),
            any(),
            eq(Stripe3ds2FingerprintTest.DS_RSA_PUBLIC_KEY),
            eq(PaymentIntentFixtures.KEY_ID))
        verify(challengeFlowStarter)
            .start(any())

        verify(activity).startActivity(eq(
            Intent(activity, ChallengeProgressDialogActivity::class.java)))
    }

    @Test
    fun handleNextAction_whenSdk3ds1() {
        controller.handleNextAction(host, PaymentIntentFixtures.PI_REQUIRES_3DS1,
            REQUEST_OPTIONS)
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

        verify(fireAndForgetRequestExecutor)
            .executeAsync(apiRequestArgumentCaptor.capture())
        assertEquals("stripe_android.3ds1_sdk",
            requireNotNull(apiRequestArgumentCaptor.firstValue.params)[AnalyticsDataFactory.FIELD_EVENT])
    }

    @Test
    fun handleNextAction_whenBrowser3ds1() {
        controller.handleNextAction(host, PaymentIntentFixtures.PI_REQUIRES_REDIRECT,
            REQUEST_OPTIONS)
        verify(activity).startActivityForResult(intentArgumentCaptor.capture(),
            eq(StripePaymentController.PAYMENT_REQUEST_CODE))
        val args: PaymentAuthWebViewStarter.Args = requireNotNull(
            intentArgumentCaptor.firstValue.getParcelableExtra(PaymentAuthWebViewStarter.EXTRA_ARGS)
        )
        assertEquals(
            "https://hooks.stripe.com/3d_secure_2_eap/begin_test/src_1Ecaz6CRMbs6FrXfuYKBRSUG/src_client_secret_F6octeOshkgxT47dr0ZxSZiv",
            args.url
        )
        assertEquals("stripe://deeplink", args.returnUrl)

        verify(fireAndForgetRequestExecutor)
            .executeAsync(apiRequestArgumentCaptor.capture())
        val analyticsParams = requireNotNull(apiRequestArgumentCaptor.firstValue.params)
        assertEquals("stripe_android.url_redirect_next_action",
            analyticsParams[AnalyticsDataFactory.FIELD_EVENT])
        assertEquals("pi_1EZlvVCRMbs6FrXfKpq2xMmy",
            analyticsParams[AnalyticsDataFactory.FIELD_INTENT_ID])
    }

    @Test
    fun handleNextAction_when3dsRedirectWithSetupIntent() {
        controller.handleNextAction(host, SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT,
            REQUEST_OPTIONS)
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
        assertEquals(StripePaymentController.PAYMENT_REQUEST_CODE,
            StripePaymentController.getRequestCode(PaymentIntentFixtures.PI_REQUIRES_3DS1))
        assertEquals(StripePaymentController.SETUP_REQUEST_CODE,
            StripePaymentController.getRequestCode(SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT))
    }

    @Test
    fun getRequestCode_withParams_correctCodeReturned() {
        assertEquals(StripePaymentController.PAYMENT_REQUEST_CODE,
            StripePaymentController.getRequestCode(
                ConfirmPaymentIntentParams.createWithPaymentMethodId(
                    "pm_123", "client_secret", "")))
    }

    @Test
    fun test3ds2Receiver_whenCompleted_shouldFireAnalyticsRequest() {
        val completionEvent = object : CompletionEvent {
            override val sdkTransactionId: String = "8dd3413f-0b45-4234-bc45-6cc40fb1b0f1"

            override val transactionStatus: String = "C"
        }

        `when`<String>(transaction.initialChallengeUiType).thenReturn("04")

        StripePaymentController.PaymentAuth3ds2ChallengeStatusReceiver(
            FakeStripeRepository(), PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            "src_123", REQUEST_OPTIONS, fireAndForgetRequestExecutor,
            analyticsDataFactory, transaction, complete3ds2AuthCallbackFactory)
            .completed(completionEvent, "01")

        verify(fireAndForgetRequestExecutor, times(2))
            .executeAsync(apiRequestArgumentCaptor.capture())
        val analyticsRequests = apiRequestArgumentCaptor.allValues

        assertEquals("stripe_android.3ds2_challenge_flow_completed",
            requireNotNull(analyticsRequests[0].params)[AnalyticsDataFactory.FIELD_EVENT])

        val analyticsParamsSecond = requireNotNull(analyticsRequests[1].params)
        assertEquals("stripe_android.3ds2_challenge_flow_presented",
            analyticsParamsSecond[AnalyticsDataFactory.FIELD_EVENT])
        assertEquals("oob",
            analyticsParamsSecond[AnalyticsDataFactory.FIELD_3DS2_UI_TYPE])
    }

    @Test
    fun test3ds2Receiver_whenTimedout_shouldFireAnalyticsRequest() {
        StripePaymentController.PaymentAuth3ds2ChallengeStatusReceiver(
            FakeStripeRepository(), PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            "src_123", ApiRequest.Options(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            fireAndForgetRequestExecutor, analyticsDataFactory, transaction,
            complete3ds2AuthCallbackFactory)
            .timedout("01")
        verify(fireAndForgetRequestExecutor, times(2))
            .executeAsync(apiRequestArgumentCaptor.capture())
        val analyticsRequests = apiRequestArgumentCaptor.allValues

        assertEquals("stripe_android.3ds2_challenge_flow_timed_out",
            requireNotNull(analyticsRequests[0].params)[AnalyticsDataFactory.FIELD_EVENT])

        assertEquals("stripe_android.3ds2_challenge_flow_presented",
            requireNotNull(analyticsRequests[1].params)[AnalyticsDataFactory.FIELD_EVENT])
    }

    @Test
    fun test3ds2Receiver_whenCanceled_shouldFireAnalyticsRequest() {
        StripePaymentController.PaymentAuth3ds2ChallengeStatusReceiver(
            FakeStripeRepository(), PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            "src_123", ApiRequest.Options(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            fireAndForgetRequestExecutor, analyticsDataFactory, transaction,
            complete3ds2AuthCallbackFactory)
            .cancelled("01")

        verify(fireAndForgetRequestExecutor, times(2))
            .executeAsync(apiRequestArgumentCaptor.capture())
        val analyticsRequests = apiRequestArgumentCaptor.allValues

        assertEquals("stripe_android.3ds2_challenge_flow_canceled",
            requireNotNull(analyticsRequests[0].params)[AnalyticsDataFactory.FIELD_EVENT])

        assertEquals("stripe_android.3ds2_challenge_flow_presented",
            requireNotNull(analyticsRequests[1].params)[AnalyticsDataFactory.FIELD_EVENT])
    }

    @Test
    fun test3ds2Receiver_whenRuntimeErrorError_shouldFireAnalyticsRequest() {
        val runtimeErrorEvent = object : RuntimeErrorEvent {
            override val errorCode: String = "404"
            override val errorMessage: String = "Resource not found"
        }

        StripePaymentController.PaymentAuth3ds2ChallengeStatusReceiver(FakeStripeRepository(),
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2, "src_123",
            ApiRequest.Options(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            fireAndForgetRequestExecutor, analyticsDataFactory, transaction,
            complete3ds2AuthCallbackFactory)
            .runtimeError(runtimeErrorEvent)

        verify(fireAndForgetRequestExecutor, times(2))
            .executeAsync(apiRequestArgumentCaptor.capture())
        val analyticsRequests = apiRequestArgumentCaptor.allValues

        val analyticsParamsFirst = requireNotNull(analyticsRequests[0].params)
        assertEquals("stripe_android.3ds2_challenge_flow_errored",
            analyticsParamsFirst[AnalyticsDataFactory.FIELD_EVENT])

        val errorData =
            analyticsParamsFirst[AnalyticsDataFactory.FIELD_ERROR_DATA] as Map<String, String>

        assertEquals("404", errorData["error_code"])
        assertEquals("Resource not found", errorData["error_message"])

        assertEquals("stripe_android.3ds2_challenge_flow_presented",
            requireNotNull(analyticsRequests[1].params)[AnalyticsDataFactory.FIELD_EVENT])
    }

    @Test
    fun test3ds2Receiver_whenProtocolError_shouldFireAnalyticsRequest() {
        val protocolErrorEvent = object : ProtocolErrorEvent {
            override val sdkTransactionId: String = "8dd3413f-0b45-4234-bc45-6cc40fb1b0f1"

            override val errorMessage: ErrorMessage = object : ErrorMessage {
                override val errorCode: String = "201"
                override val errorDescription: String = "Required element missing"
                override val errorDetails: String = "eci"
                override val transactionId: String = "047f76a6-d1d4-48a2-aa65-786abb6f7f46"
            }
        }

        StripePaymentController.PaymentAuth3ds2ChallengeStatusReceiver(FakeStripeRepository(),
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2, "src_123",
            ApiRequest.Options(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            fireAndForgetRequestExecutor, analyticsDataFactory, transaction,
            complete3ds2AuthCallbackFactory)
            .protocolError(protocolErrorEvent)

        verify(fireAndForgetRequestExecutor, times(2))
            .executeAsync(apiRequestArgumentCaptor.capture())
        val analyticsRequests = apiRequestArgumentCaptor.allValues

        val analyticsParamsFirst = requireNotNull(analyticsRequests[0].params)
        assertEquals("stripe_android.3ds2_challenge_flow_errored",
            analyticsParamsFirst[AnalyticsDataFactory.FIELD_EVENT])

        val errorData =
            analyticsParamsFirst[AnalyticsDataFactory.FIELD_ERROR_DATA] as Map<String, String>

        assertEquals("201", errorData["error_code"])

        assertEquals("stripe_android.3ds2_challenge_flow_presented",
            requireNotNull(analyticsRequests[1].params)[AnalyticsDataFactory.FIELD_EVENT])
    }

    @Test
    fun test3ds2Completion_whenCanceled_shouldCallStarterWithCancelStatus() {
        val receiver = StripePaymentController.PaymentAuth3ds2ChallengeStatusReceiver(
            FakeStripeRepository(),
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2, "src_123",
            ApiRequest.Options(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            fireAndForgetRequestExecutor, analyticsDataFactory, transaction,
            complete3ds2AuthCallbackFactory
        )
        receiver.cancelled("01")
        verify(complete3ds2AuthCallback).onSuccess(true)
    }

    @Test
    fun getClientSecret_shouldGetClientSecretFromIntent() {
        val data = Intent()
            .putExtra(StripeIntentResultExtras.CLIENT_SECRET,
                PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2.clientSecret)
        assertNotNull(PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2.clientSecret)
        assertEquals(PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2.clientSecret,
            controller.getClientSecret(data))
    }

    @Test
    fun handlePaymentResult_withAuthException_shouldCallCallbackOnError() {
        val exception = RuntimeException()
        val intent = Intent()
            .putExtra(StripeIntentResultExtras.AUTH_EXCEPTION, exception)

        controller.handlePaymentResult(intent, REQUEST_OPTIONS, paymentAuthResultCallback)
        verify(paymentAuthResultCallback).onError(exception)
        verify(paymentAuthResultCallback, never())
            .onSuccess(anyOrNull())
    }

    @Test
    fun handleSetupResult_withAuthException_shouldCallCallbackOnError() {
        val exception = RuntimeException()
        val intent = Intent()
            .putExtra(StripeIntentResultExtras.AUTH_EXCEPTION, exception)

        controller.handleSetupResult(intent, REQUEST_OPTIONS, setupAuthResultCallback)

        verify(setupAuthResultCallback).onError(exception)
        verify(setupAuthResultCallback, never())
            .onSuccess(anyOrNull())
    }

    @Test
    fun handleSetupResult_shouldCallbackOnSuccess() {
        assertNotNull(SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT.clientSecret)

        val intent = Intent()
            .putExtra(StripeIntentResultExtras.FLOW_OUTCOME,
                StripeIntentResult.Outcome.SUCCEEDED)
            .putExtra(StripeIntentResultExtras.CLIENT_SECRET,
                SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT.clientSecret)

        controller.handleSetupResult(intent, REQUEST_OPTIONS, setupAuthResultCallback)

        verify(setupAuthResultCallback).onSuccess(resultCaptor.capture())
        val result = resultCaptor.firstValue
        assertEquals(StripeIntentResult.Outcome.SUCCEEDED, result.outcome)
        assertEquals(SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT, result.intent)
    }

    @Test
    fun authCallback_withChallengeFlow_shouldNotStartRelayActivity() {
        val authCallback = StripePaymentController.Stripe3ds2AuthCallback(
            host, FakeStripeRepository(), transaction, MAX_TIMEOUT,
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2, SOURCE_ID,
            REQUEST_OPTIONS, fireAndForgetRequestExecutor, analyticsDataFactory,
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
            REQUEST_OPTIONS, fireAndForgetRequestExecutor, analyticsDataFactory,
            challengeFlowStarter,
            paymentRelayStarter = paymentRelayStarter
        )
        authCallback.onSuccess(Stripe3ds2AuthResultFixtures.ARES_FRICTIONLESS_FLOW)
        verify(paymentRelayStarter)
            .start(relayStarterArgsArgumentCaptor.capture())
        assertEquals(PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            relayStarterArgsArgumentCaptor.firstValue.stripeIntent)

        verify(fireAndForgetRequestExecutor).executeAsync(apiRequestArgumentCaptor.capture())
        val analyticsRequest = apiRequestArgumentCaptor.firstValue
        val analyticsParams = requireNotNull(analyticsRequest.params)
        assertEquals("stripe_android.3ds2_frictionless_flow",
            analyticsParams[AnalyticsDataFactory.FIELD_EVENT])
        assertEquals("pi_1ExkUeAWhjPjYwPiXph9ouXa",
            analyticsParams[AnalyticsDataFactory.FIELD_INTENT_ID])
    }

    @Test
    fun authCallback_withFallbackRedirectUrl_shouldStartAuthWebView() {
        val authCallback = StripePaymentController.Stripe3ds2AuthCallback(
            host, FakeStripeRepository(), transaction, MAX_TIMEOUT,
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2, SOURCE_ID,
            REQUEST_OPTIONS, fireAndForgetRequestExecutor, analyticsDataFactory,
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

        verify(fireAndForgetRequestExecutor).executeAsync(apiRequestArgumentCaptor.capture())
        val analyticsRequest = apiRequestArgumentCaptor.firstValue
        assertEquals("stripe_android.3ds2_fallback",
            requireNotNull(analyticsRequest.params)[AnalyticsDataFactory.FIELD_EVENT])
    }

    @Test
    fun authCallback_withError_shouldStartRelayActivityWithException() {
        val authCallback = StripePaymentController.Stripe3ds2AuthCallback(
            host, FakeStripeRepository(), transaction, MAX_TIMEOUT,
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2, SOURCE_ID,
            REQUEST_OPTIONS, fireAndForgetRequestExecutor, analyticsDataFactory,
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

    private class FakeStripeRepository : AbsFakeStripeRepository() {
        override fun retrieveSetupIntent(
            clientSecret: String,
            options: ApiRequest.Options
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
            callback: ApiResultCallback<Boolean>
        ) {
            callback.onSuccess(true)
        }

        override fun retrieveIntent(
            clientSecret: String,
            options: ApiRequest.Options,
            callback: ApiResultCallback<StripeIntent>
        ) {
            super.retrieveIntent(clientSecret, options, callback)
            callback.onSuccess(SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT)
        }
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
