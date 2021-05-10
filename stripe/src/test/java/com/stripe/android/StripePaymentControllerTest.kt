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
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.stripe.android.StripePaymentController.Companion.EXPAND_PAYMENT_METHOD
import com.stripe.android.auth.PaymentBrowserAuthContract
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
import com.stripe.android.networking.AnalyticsEvent
import com.stripe.android.networking.AnalyticsRequest
import com.stripe.android.networking.AnalyticsRequestExecutor
import com.stripe.android.networking.AnalyticsRequestFactory
import com.stripe.android.networking.ApiRequest
import com.stripe.android.payments.DefaultReturnUrl
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.stripe3ds2.service.StripeThreeDs2Service
import com.stripe.android.stripe3ds2.transaction.ChallengeParameters
import com.stripe.android.stripe3ds2.transaction.MessageVersionRegistry
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
import kotlinx.coroutines.test.resetMain
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
    private val paymentRelayStarter: PaymentRelayStarter = mock()
    private val analyticsRequestExecutor: AnalyticsRequestExecutor = mock()
    private val challengeProgressActivityStarter: StripePaymentController.ChallengeProgressActivityStarter =
        mock()
    private val alipayRepository = mock<AlipayRepository>()
    private val stripeRepository = FakeStripeRepository()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val analyticsRequestFactory = AnalyticsRequestFactory(
        context,
        ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
    )
    private val host = AuthActivityStarter.Host.create(activity)

    private val relayStarterArgsArgumentCaptor: KArgumentCaptor<PaymentRelayStarter.Args> =
        argumentCaptor()
    private val intentArgumentCaptor: KArgumentCaptor<Intent> = argumentCaptor()
    private val analyticsRequestArgumentCaptor: KArgumentCaptor<AnalyticsRequest> = argumentCaptor()

    private val testDispatcher = TestCoroutineDispatcher()

    private val defaultReturnUrl = DefaultReturnUrl.create(context)
    private val paymentBrowserAuthContract = PaymentBrowserAuthContract(
        defaultReturnUrl,
        hasCompatibleBrowser = { true }
    )
    private val controller = createController()

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
    internal fun handleNextAction_withMastercardAnd3ds2_shouldStart3ds2ChallengeFlow() =
        testDispatcher.runBlockingTest {
            val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2
            val dsPublicKey =
                Stripe3ds2Fingerprint(paymentIntent.nextActionData as StripeIntent.NextActionData.SdkData.Use3DS2)
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
            controller.handleNextAction(
                host,
                paymentIntent,
                null,
                REQUEST_OPTIONS
            )
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
            assertThat(analyticsParams[AnalyticsRequestFactory.FIELD_EVENT])
                .isEqualTo(AnalyticsEvent.Auth3ds2Fingerprint.toString())
        }

    @Test
    fun handleNextAction_withAmexAnd3ds2_shouldStart3ds2ChallengeFlow() =
        testDispatcher.runBlockingTest {
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
                null,
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
    fun handleNextAction_whenSdk3ds1() = testDispatcher.runBlockingTest {
        controller.handleNextAction(
            host,
            PaymentIntentFixtures.PI_REQUIRES_3DS1,
            null,
            REQUEST_OPTIONS
        )
        verify(activity).startActivityForResult(
            intentArgumentCaptor.capture(),
            eq(StripePaymentController.PAYMENT_REQUEST_CODE)
        )
        val args = requireNotNull(
            PaymentBrowserAuthContract.parseArgs(intentArgumentCaptor.firstValue)
        )
        assertThat(args.url)
            .isEqualTo("https://hooks.stripe.com/3d_secure_2_eap/begin_test/src_1Ecve7CRMbs6FrXfm8AxXMIh/src_client_secret_F79yszOBAiuaZTuIhbn3LPUW")
        assertThat(args.returnUrl).isNull()

        verifyAnalytics(AnalyticsEvent.Auth3ds1Sdk)
    }

    @Test
    fun handleNextAction_whenSdk3ds1_withReturnUrl() = testDispatcher.runBlockingTest {
        controller.handleNextAction(
            host,
            PaymentIntentFixtures.PI_REQUIRES_3DS1,
            defaultReturnUrl.value,
            REQUEST_OPTIONS
        )
        verify(activity).startActivityForResult(
            intentArgumentCaptor.capture(),
            eq(StripePaymentController.PAYMENT_REQUEST_CODE)
        )
        val args = requireNotNull(
            PaymentBrowserAuthContract.parseArgs(intentArgumentCaptor.firstValue)
        )
        assertThat(args.url)
            .isEqualTo("https://hooks.stripe.com/3d_secure_2_eap/begin_test/src_1Ecve7CRMbs6FrXfm8AxXMIh/src_client_secret_F79yszOBAiuaZTuIhbn3LPUW")
        assertThat(args.returnUrl)
            .isEqualTo(defaultReturnUrl.value)

        verifyAnalytics(AnalyticsEvent.Auth3ds1Sdk)
    }

    @Test
    fun handleNextAction_whenBrowser3ds1() = testDispatcher.runBlockingTest {
        controller.handleNextAction(
            host,
            PaymentIntentFixtures.PI_REQUIRES_REDIRECT,
            null,
            REQUEST_OPTIONS
        )
        verify(activity).startActivityForResult(
            intentArgumentCaptor.capture(),
            eq(StripePaymentController.PAYMENT_REQUEST_CODE)
        )
        val args = requireNotNull(
            PaymentBrowserAuthContract.parseArgs(intentArgumentCaptor.firstValue)
        )
        assertThat(args.url)
            .isEqualTo("https://hooks.stripe.com/3d_secure_2_eap/begin_test/src_1Ecaz6CRMbs6FrXfuYKBRSUG/src_client_secret_F6octeOshkgxT47dr0ZxSZiv")
        assertThat(args.returnUrl).isEqualTo("stripe://deeplink")

        verify(analyticsRequestExecutor)
            .executeAsync(analyticsRequestArgumentCaptor.capture())
        val analyticsParams = requireNotNull(analyticsRequestArgumentCaptor.firstValue.params)
        assertThat(analyticsParams[AnalyticsRequestFactory.FIELD_EVENT])
            .isEqualTo(AnalyticsEvent.AuthRedirect.toString())
    }

    @Test
    fun handleNextAction_when3dsRedirectWithSetupIntent() = testDispatcher.runBlockingTest {
        controller.handleNextAction(
            host,
            SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT,
            null,
            REQUEST_OPTIONS
        )
        verify(activity).startActivityForResult(
            any(),
            eq(StripePaymentController.SETUP_REQUEST_CODE)
        )
    }

    @Test
    fun `handleNextAction oxxo details`() = testDispatcher.runBlockingTest {
        controller.handleNextAction(
            host,
            PaymentIntentFixtures.OXXO_REQUIES_ACTION,
            null,
            REQUEST_OPTIONS
        )
        verify(activity).startActivityForResult(
            intentArgumentCaptor.capture(),
            eq(StripePaymentController.PAYMENT_REQUEST_CODE)
        )
        val args = requireNotNull(
            PaymentBrowserAuthContract.parseArgs(intentArgumentCaptor.firstValue)
        )
        assertThat(args.url)
            .isEqualTo("https://payments.stripe.com/oxxo/voucher/test_YWNjdF8xSWN1c1VMMzJLbFJvdDAxLF9KRlBtckVBMERWM0lBZEUyb")
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
                    "client_secret"
                )
            )
        ).isEqualTo(StripePaymentController.PAYMENT_REQUEST_CODE)
    }

    @Test
    fun `on3ds2AuthSuccess() with challenge flow should not start relay activity`() =
        testDispatcher.runBlockingTest {
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
    fun `on3ds2AuthSuccess() with frictionless flow should start relay activity with PaymentIntent`() =
        testDispatcher.runBlockingTest {
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
            val args =
                relayStarterArgsArgumentCaptor.firstValue as? PaymentRelayStarter.Args.PaymentIntentArgs
            assertThat(args?.paymentIntent)
                .isEqualTo(PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2)

            verify(analyticsRequestExecutor)
                .executeAsync(analyticsRequestArgumentCaptor.capture())
            val analyticsRequest = analyticsRequestArgumentCaptor.firstValue
            val analyticsParams = requireNotNull(analyticsRequest.params)
            assertThat(analyticsParams[AnalyticsRequestFactory.FIELD_EVENT])
                .isEqualTo(AnalyticsEvent.Auth3ds2Frictionless.toString())
        }

    @Test
    fun `on3ds2AuthSuccess() with fallback redirect URL should start auth webview activity`() =
        testDispatcher.runBlockingTest {
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
                PaymentBrowserAuthContract.parseArgs(intentArgumentCaptor.firstValue)
            )
            assertThat(args.url)
                .isEqualTo("https://hooks.stripe.com/3d_secure_2_eap/begin_test/src_1Ecve7CRMbs6FrXfm8AxXMIh/src_client_secret_F79yszOBAiuaZTuIhbn3LPUW")
            assertThat(args.returnUrl).isNull()

            verify(analyticsRequestExecutor)
                .executeAsync(analyticsRequestArgumentCaptor.capture())
            val analyticsRequest = analyticsRequestArgumentCaptor.firstValue
            assertThat(
                requireNotNull(analyticsRequest.params)[AnalyticsRequestFactory.FIELD_EVENT]
            ).isEqualTo(AnalyticsEvent.Auth3ds2Fallback.toString())
        }

    @Test
    fun `on3ds2AuthSuccess() with AReq error should start relay activity with exception`() =
        testDispatcher.runBlockingTest {
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

            val args =
                relayStarterArgsArgumentCaptor.firstValue as? PaymentRelayStarter.Args.ErrorArgs
            assertThat(args?.exception?.message).isEqualTo(
                "Error encountered during 3DS2 authentication request. " +
                    "Code: 302, Detail: null, " +
                    "Description: Data could not be decrypted by the receiving system due to " +
                    "technical or other reason., Component: D"
            )
        }

    @Test
    fun `startChallengeFlow() when successful should call doChallenge()`() =
        testDispatcher.runBlockingTest {
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
    fun `startChallengeFlow() when failure should start relay activity with exception()`() =
        testDispatcher.runBlockingTest {
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
                argWhere<PaymentRelayStarter.Args.ErrorArgs> {
                    it.exception.message == "Error while attempting to start 3DS2 challenge flow."
                }
            )
        }

    @Test
    fun handlePaymentResult_whenSourceShouldBeCanceled_onlyCallsCancelIntentOnce() =
        testDispatcher.runBlockingTest {
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
                    canCancelSource = true,
                    stripeAccountId = ACCOUNT_ID
                ).toBundle()
            )

            val paymentIntentResult = controller.getPaymentIntentResult(intent)

            assertThat(stripeRepository.retrievePaymentIntentArgs)
                .containsExactly(
                    Triple(clientSecret, REQUEST_OPTIONS, listOf("payment_method"))
                )

            // verify that cancelIntent is only called once
            assertThat(stripeRepository.cancelPaymentIntentArgs)
                .containsExactly(
                    Triple(paymentIntent.id.orEmpty(), sourceId, REQUEST_OPTIONS)
                )

            assertThat(paymentIntentResult).isEqualTo(PaymentIntentResult(paymentIntent))
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
    fun startAuthenticateSource_withNoneFlowSource_shouldBypassAuth() =
        testDispatcher.runBlockingTest {
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
            canCancelSource = true
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
    fun `confirmAndAuthenticateAlipay() should return expected outcome`() =
        testDispatcher.runBlockingTest {
            whenever(alipayRepository.authenticate(any(), any(), any())).thenReturn(
                AlipayAuthResult(
                    StripeIntentResult.Outcome.SUCCEEDED
                )
            )
            stripeRepository.retrievePaymentIntentResponse =
                PaymentIntentFixtures.ALIPAY_REQUIRES_ACTION

            val actualResponse = controller.confirmAndAuthenticateAlipay(
                ConfirmPaymentIntentParams.createWithPaymentMethodId(
                    "pm_123",
                    "client_secret",
                ),
                mock(),
                REQUEST_OPTIONS
            )

            assertThat(stripeRepository.confirmPaymentIntentArgs).hasSize(1)
            assertThat(stripeRepository.confirmPaymentIntentArgs[0].first.shouldUseStripeSdk()).isTrue()
            assertThat(stripeRepository.confirmPaymentIntentArgs[0].second).isSameInstanceAs(
                REQUEST_OPTIONS
            )
            assertThat(stripeRepository.confirmPaymentIntentArgs[0].third).isSameInstanceAs(
                EXPAND_PAYMENT_METHOD
            )
            assertThat(actualResponse.intent).isEqualTo(PaymentIntentFixtures.ALIPAY_REQUIRES_ACTION)
            assertThat(actualResponse.outcome).isEqualTo(StripeIntentResult.Outcome.SUCCEEDED)
        }

    @Test
    fun `bypassAuth() with ActivityResultLauncher should use ActivityResultLauncher`() =
        testDispatcher.runBlockingTest {
            verify(activity).window

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
    fun `on3ds2AuthFallback() with ActivityResultLauncher should use ActivityResultLauncher`() =
        testDispatcher.runBlockingTest {
            verify(activity).window

            val launcher = FakeActivityResultLauncher(paymentBrowserAuthContract)
            createController(
                paymentBrowserAuthLauncher = launcher
            ).on3ds2AuthFallback(
                "https://example.com",
                host,
                PaymentIntentFixtures.PI_WITH_LAST_PAYMENT_ERROR,
                REQUEST_OPTIONS
            )
            assertThat(launcher.launchArgs)
                .containsExactly(
                    PaymentBrowserAuthContract.Args(
                        objectId = "pi_1F7J1aCRMbs6FrXfaJcvbxF6",
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
        paymentBrowserAuthLauncher: ActivityResultLauncher<PaymentBrowserAuthContract.Args>? = null
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
            analyticsRequestFactory,
            challengeProgressActivityStarter,
            alipayRepository,
            paymentRelayLauncher = paymentRelayLauncher,
            paymentBrowserAuthLauncher = paymentBrowserAuthLauncher,
            workContext = testDispatcher,
            uiContext = testDispatcher
        )
    }

    private class FakeStripeRepository : AbsFakeStripeRepository() {
        var retrievePaymentIntentResponse = PaymentIntentFixtures.PI_REQUIRES_REDIRECT
        var cancelPaymentIntentResponse = PaymentIntentFixtures.CANCELLED
        var confirmPaymentIntentResponse = PaymentIntentFixtures.PI_WITH_SHIPPING

        val retrievePaymentIntentArgs =
            mutableListOf<Triple<String, ApiRequest.Options, List<String>>>()
        val cancelPaymentIntentArgs = mutableListOf<Triple<String, String, ApiRequest.Options>>()
        val confirmPaymentIntentArgs =
            mutableListOf<Triple<ConfirmPaymentIntentParams, ApiRequest.Options, List<String>>>()

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

        override suspend fun confirmPaymentIntent(
            confirmPaymentIntentParams: ConfirmPaymentIntentParams,
            options: ApiRequest.Options,
            expandFields: List<String>
        ): PaymentIntent {
            confirmPaymentIntentArgs.add(
                Triple(confirmPaymentIntentParams, options, expandFields)
            )
            return confirmPaymentIntentResponse
        }
    }

    private fun verifyAnalytics(event: AnalyticsEvent) {
        verify(analyticsRequestExecutor)
            .executeAsync(analyticsRequestArgumentCaptor.capture())
        val analyticsRequest = analyticsRequestArgumentCaptor.firstValue
        assertThat(
            analyticsRequest.compactParams?.get(AnalyticsRequestFactory.FIELD_EVENT)
        ).isEqualTo(event.toString())
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
