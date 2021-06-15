package com.stripe.android.payments.core.authentication

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.Logger
import com.stripe.android.PaymentBrowserAuthStarter
import com.stripe.android.StripePaymentController.Companion.PAYMENT_REQUEST_CODE
import com.stripe.android.StripePaymentController.Companion.SETUP_REQUEST_CODE
import com.stripe.android.auth.PaymentBrowserAuthContract
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.AnalyticsEvent
import com.stripe.android.networking.AnalyticsRequest
import com.stripe.android.networking.AnalyticsRequestExecutor
import com.stripe.android.networking.AnalyticsRequestFactory
import com.stripe.android.networking.ApiRequest
import com.stripe.android.view.AuthActivityStarterHost
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
class WebIntentAuthenticatorTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val paymentBrowserAuthStarterFactory =
        mock<(AuthActivityStarterHost) -> PaymentBrowserAuthStarter>()
    private val analyticsRequestExecutor = mock<AnalyticsRequestExecutor>()
    private val analyticsRequestFactory = AnalyticsRequestFactory(
        context,
        ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
    )
    private val logger = mock<Logger>()

    private val testDispatcher = TestCoroutineDispatcher()
    private val host = mock<AuthActivityStarterHost>()

    private val paymentBrowserWebStarter = mock<PaymentBrowserAuthStarter>()

    private val browserAuthContractArgumentCaptor: KArgumentCaptor<PaymentBrowserAuthContract.Args> =
        argumentCaptor()
    private val analyticsRequestArgumentCaptor: KArgumentCaptor<AnalyticsRequest> = argumentCaptor()

    private val authenticator = WebIntentAuthenticator(
        paymentBrowserAuthStarterFactory,
        analyticsRequestExecutor,
        analyticsRequestFactory,
        logger,
        enableLogging = false,
        testDispatcher
    )

    @Before
    fun setUp() {
        whenever(paymentBrowserAuthStarterFactory(any())).thenReturn(paymentBrowserWebStarter)
    }

    @Test
    fun authenticate_whenSdk3ds1() {
        verifyAuthenticate(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_3DS1,
            expectedUrl = "https://hooks.stripe.com/3d_secure_2_eap/begin_test/src_1Ecve7CRMbs6FrXfm8AxXMIh/src_client_secret_F79yszOBAiuaZTuIhbn3LPUW",
            expectedReturnUrl = null,
            expectedRequestCode = PAYMENT_REQUEST_CODE,
            expectedAnalyticsEvent = AnalyticsEvent.Auth3ds1Sdk
        )
    }

    @Test
    fun authenticate_whenSdk3ds1_withReturnUrl() {
        verifyAuthenticate(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_3DS1,
            threeDs1ReturnUrl = RETURN_URL_FOR_3DS1,
            expectedUrl = "https://hooks.stripe.com/3d_secure_2_eap/begin_test/src_1Ecve7CRMbs6FrXfm8AxXMIh/src_client_secret_F79yszOBAiuaZTuIhbn3LPUW",
            expectedReturnUrl = RETURN_URL_FOR_3DS1,
            expectedRequestCode = PAYMENT_REQUEST_CODE,
            expectedAnalyticsEvent = AnalyticsEvent.Auth3ds1Sdk
        )
    }

    @Test
    fun authenticate_whenRedirectToUrl() {
        verifyAuthenticate(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_REDIRECT,
            expectedUrl = "https://hooks.stripe.com/3d_secure_2_eap/begin_test/src_1Ecaz6CRMbs6FrXfuYKBRSUG/src_client_secret_F6octeOshkgxT47dr0ZxSZiv",
            expectedReturnUrl = "stripe://deeplink",
            expectedRequestCode = PAYMENT_REQUEST_CODE,
            expectedAnalyticsEvent = AnalyticsEvent.AuthRedirect
        )
    }

    @Test
    fun authenticate_whenRedirectToUrlWithSetupIntent() {
        verifyAuthenticate(
            stripeIntent = SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT,
            expectedUrl = "https://hooks.stripe.com/redirect/authenticate/src_1EqTStGMT9dGPIDGJGPkqE6B?client_secret=src_client_secret_FL9m741mmxtHykDlRTC5aQ02",
            expectedReturnUrl = "stripe://setup_intent_return",
            expectedRequestCode = SETUP_REQUEST_CODE,
            expectedAnalyticsEvent = AnalyticsEvent.AuthRedirect
        )
    }

    @Test
    fun authenticate_whenDisplayOxxoDetails() {
        verifyAuthenticate(
            stripeIntent = PaymentIntentFixtures.OXXO_REQUIES_ACTION,
            expectedUrl = "https://payments.stripe.com/oxxo/voucher/test_YWNjdF8xSWN1c1VMMzJLbFJvdDAxLF9KRlBtckVBMERWM0lBZEUyb",
            expectedReturnUrl = null,
            expectedRequestCode = PAYMENT_REQUEST_CODE,
            expectedAnalyticsEvent = null,
            expectedShouldCancelIntentOnUserNavigation = false,
        )
    }

    private fun verifyAuthenticate(
        stripeIntent: StripeIntent,
        threeDs1ReturnUrl: String? = null,
        expectedUrl: String,
        expectedReturnUrl: String?,
        expectedRequestCode: Int,
        expectedShouldCancelIntentOnUserNavigation: Boolean = true,
        expectedAnalyticsEvent: AnalyticsEvent?
    ) = testDispatcher.runBlockingTest {
        authenticator.authenticate(
            host,
            stripeIntent,
            threeDs1ReturnUrl,
            REQUEST_OPTIONS
        )
        verify(paymentBrowserWebStarter).start(
            browserAuthContractArgumentCaptor.capture()
        )
        val args = requireNotNull(
            browserAuthContractArgumentCaptor.firstValue
        )

        assertThat(args.requestCode).isEqualTo(expectedRequestCode)
        assertThat(args.url).isEqualTo(expectedUrl)
        assertThat(args.returnUrl).isEqualTo(expectedReturnUrl)
        assertThat(args.shouldCancelIntentOnUserNavigation).isEqualTo(
            expectedShouldCancelIntentOnUserNavigation
        )

        expectedAnalyticsEvent?.let {
            verifyAnalytics(it)
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

        private const val RETURN_URL_FOR_3DS1 = "stripesdk://payment_return_url"
        private val REQUEST_OPTIONS = ApiRequest.Options(
            apiKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            stripeAccount = ACCOUNT_ID
        )
    }
}
