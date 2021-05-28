package com.stripe.android.payments.core.authentication

import android.app.Activity
import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth
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
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentAuthConfig
import com.stripe.android.PaymentRelayStarter
import com.stripe.android.StripePaymentController
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.Stripe3ds2AuthResultFixtures
import com.stripe.android.model.Stripe3ds2AuthResultFixtures.ARES_CHALLENGE_FLOW
import com.stripe.android.model.Stripe3ds2Fingerprint
import com.stripe.android.model.Stripe3ds2FingerprintTest
import com.stripe.android.model.Stripe3ds2Fixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.AnalyticsEvent
import com.stripe.android.networking.AnalyticsRequest
import com.stripe.android.networking.AnalyticsRequestExecutor
import com.stripe.android.networking.AnalyticsRequestFactory
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.stripe3ds2.service.StripeThreeDs2Service
import com.stripe.android.stripe3ds2.transaction.ChallengeParameters
import com.stripe.android.stripe3ds2.transaction.MessageVersionRegistry
import com.stripe.android.stripe3ds2.transaction.SdkTransactionId
import com.stripe.android.stripe3ds2.transaction.Stripe3ds2ActivityStarterHost
import com.stripe.android.stripe3ds2.transaction.Transaction
import com.stripe.android.view.AuthActivityStarter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
class Stripe3DS2AuthenticatorTest {
    private val testDispatcher = TestCoroutineDispatcher()
    private val activity: Activity = mock()
    private val host = AuthActivityStarter.Host.create(activity)

    private val sdkTransactionId = mock<SdkTransactionId>().also {
        whenever(it.value).thenReturn(UUID.randomUUID().toString())
    }
    private val stripeRepository = mock<StripeRepository>()
    private val webIntentAuthenticator = mock<WebIntentAuthenticator>()
    private val paymentRelayStarterFactory =
        mock<(AuthActivityStarter.Host) -> PaymentRelayStarter>()
    private val analyticsRequestExecutor = mock<AnalyticsRequestExecutor>()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val analyticsRequestFactory = AnalyticsRequestFactory(
        context,
        ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
    )
    private val threeDs2Service = mock<StripeThreeDs2Service>()
    private val messageVersionRegistry = MessageVersionRegistry()
    private val challengeProgressActivityStarter =
        mock<StripePaymentController.ChallengeProgressActivityStarter>()
    private val stripe3ds2Config = PaymentAuthConfig.Stripe3ds2Config.Builder()
        .setTimeout(5)
        .build()
    private val stripe3ds2ChallengeLauncher =
        mock<ActivityResultLauncher<PaymentFlowResult.Unvalidated>>()

    private val paymentRelayStarter = mock<PaymentRelayStarter>()
    private val relayStarterArgsArgumentCaptor: KArgumentCaptor<PaymentRelayStarter.Args> =
        argumentCaptor()
    private val analyticsRequestArgumentCaptor: KArgumentCaptor<AnalyticsRequest> = argumentCaptor()

    private val transaction = mock<Transaction>()

    private val authenticator = Stripe3DS2Authenticator(
        stripeRepository,
        webIntentAuthenticator,
        paymentRelayStarterFactory,
        analyticsRequestExecutor,
        analyticsRequestFactory,
        threeDs2Service,
        messageVersionRegistry,
        challengeProgressActivityStarter,
        stripe3ds2Config,
        stripe3ds2ChallengeLauncher,
        testDispatcher,
        testDispatcher
    )

    @Before
    fun setUp() {
        whenever(transaction.sdkTransactionId)
            .thenReturn(sdkTransactionId)
        runBlocking {
            whenever(transaction.createAuthenticationRequestParameters())
                .thenReturn(Stripe3ds2Fixtures.createAreqParams(sdkTransactionId))
            whenever(
                stripeRepository.start3ds2Auth(
                    any(),
                    any()
                )
            ).thenReturn(
                ARES_CHALLENGE_FLOW
            )
        }
    }

    @Test
    fun `on3ds2AuthSuccess() with challenge flow should not start relay activity`() {
        testDispatcher.runBlockingTest {
            authenticator.on3ds2AuthSuccess(
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
    }

    @Test
    fun `on3ds2AuthSuccess() with frictionless flow should start relay activity with PaymentIntent`() =
        testDispatcher.runBlockingTest {
            authenticator.on3ds2AuthSuccess(
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
    fun `on3ds2AuthSuccess() with fallback redirect URL should send analyticsRequest invoke webAuthenticator`() =
        testDispatcher.runBlockingTest {
            authenticator.on3ds2AuthSuccess(
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

            verify(analyticsRequestExecutor)
                .executeAsync(analyticsRequestArgumentCaptor.capture())
            val analyticsRequest = analyticsRequestArgumentCaptor.firstValue
            Truth.assertThat(
                requireNotNull(analyticsRequest.params)[AnalyticsRequestFactory.FIELD_EVENT]
            ).isEqualTo(AnalyticsEvent.Auth3ds2Fallback.toString())

            verify(webIntentAuthenticator).beginWebAuth(
                host,
                PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
                StripePaymentController.getRequestCode(PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2),
                PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2.clientSecret.orEmpty(),
                Stripe3ds2AuthResultFixtures.FALLBACK_REDIRECT_URL.fallbackRedirectUrl!!,
                REQUEST_OPTIONS.stripeAccount,
                shouldCancelSource = true
            )
        }

    @Test
    fun `on3ds2AuthSuccess() with AReq error should start relay activity with exception`() =
        testDispatcher.runBlockingTest {
            authenticator.on3ds2AuthSuccess(
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
            Truth.assertThat(args?.exception?.message).isEqualTo(
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
            authenticator.startChallengeFlow(
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
            authenticator.startChallengeFlow(
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
    fun authenticate_withMastercardAnd3ds2_shouldStart3ds2ChallengeFlow() =
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
            ).thenReturn(transaction)

            authenticator.authenticate(
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
    fun authenticate_withAmexAnd3ds2_shouldStart3ds2ChallengeFlow() =
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
            ).thenReturn(transaction)

            authenticator.authenticate(
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

    private companion object {
        private const val ACCOUNT_ID = "acct_123"
        private const val SOURCE_ID = "src_123"
        private const val MAX_TIMEOUT = 5
        private val REQUEST_OPTIONS = ApiRequest.Options(
            apiKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            stripeAccount = ACCOUNT_ID
        )

        private const val MESSAGE_VERSION = Stripe3ds2Fixtures.MESSAGE_VERSION
        private const val MASTERCARD_DS_ID = "A000000004"
        private const val AMEX_DS_ID = "A000000025"
    }
}
