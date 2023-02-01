package com.stripe.android.payments.core.authentication

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentBrowserAuthStarter
import com.stripe.android.StripePaymentController.Companion.PAYMENT_REQUEST_CODE
import com.stripe.android.StripePaymentController.Companion.SETUP_REQUEST_CODE
import com.stripe.android.auth.PaymentBrowserAuthContract
import com.stripe.android.core.networking.AnalyticsFields
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.PaymentAnalyticsEvent
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.payments.DefaultReturnUrl
import com.stripe.android.view.AuthActivityStarterHost
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WebIntentAuthenticatorTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val paymentBrowserAuthStarterFactory =
        mock<(AuthActivityStarterHost) -> PaymentBrowserAuthStarter>()
    private val analyticsRequestExecutor = mock<AnalyticsRequestExecutor>()
    private val analyticsRequestFactory = PaymentAnalyticsRequestFactory(
        context,
        ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
    )

    private val testDispatcher = UnconfinedTestDispatcher()
    private val host = mock<AuthActivityStarterHost> {
        on { lifecycleOwner } doReturn TestLifecycleOwner(initialState = Lifecycle.State.RESUMED)
    }

    private val paymentBrowserWebStarter = mock<PaymentBrowserAuthStarter>()

    private val browserAuthContractArgumentCaptor: KArgumentCaptor<PaymentBrowserAuthContract.Args> =
        argumentCaptor()
    private val analyticsRequestArgumentCaptor: KArgumentCaptor<AnalyticsRequest> = argumentCaptor()

    private var threeDs1IntentReturnUrlMap = mutableMapOf<String, String>()

    private val authenticator = WebIntentAuthenticator(
        paymentBrowserAuthStarterFactory = paymentBrowserAuthStarterFactory,
        analyticsRequestExecutor = analyticsRequestExecutor,
        paymentAnalyticsRequestFactory = analyticsRequestFactory,
        enableLogging = false,
        uiContext = testDispatcher,
        threeDs1IntentReturnUrlMap = threeDs1IntentReturnUrlMap,
        publishableKeyProvider = { ApiKeyFixtures.FAKE_PUBLISHABLE_KEY },
        isInstantApp = false,
        defaultReturnUrl = DefaultReturnUrl("some_package_name"),
    )

    @Before
    fun setUp() {
        threeDs1IntentReturnUrlMap[PAYMENT_INTENT_ID_FOR_3DS1] = RETURN_URL_FOR_3DS1
        whenever(paymentBrowserAuthStarterFactory(any())).thenReturn(paymentBrowserWebStarter)
    }

    @Test
    fun authenticate_whenSdk3ds1() {
        threeDs1IntentReturnUrlMap.remove(PAYMENT_INTENT_ID_FOR_3DS1)
        verifyAuthenticate(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_3DS1,
            expectedUrl = "https://hooks.stripe.com/3d_secure_2_eap/begin_test/src_1Ecve7CRMbs6FrXfm8AxXMIh/src_client_secret_F79yszOBAiuaZTuIhbn3LPUW",
            expectedReturnUrl = null,
            expectedRequestCode = PAYMENT_REQUEST_CODE,
            expectedAnalyticsEvent = PaymentAnalyticsEvent.Auth3ds1Sdk
        )
    }

    @Test
    fun authenticate_whenSdk3ds1_withReturnUrl() {
        verifyAuthenticate(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_3DS1,
            expectedUrl = "https://hooks.stripe.com/3d_secure_2_eap/begin_test/src_1Ecve7CRMbs6FrXfm8AxXMIh/src_client_secret_F79yszOBAiuaZTuIhbn3LPUW",
            expectedReturnUrl = RETURN_URL_FOR_3DS1,
            expectedRequestCode = PAYMENT_REQUEST_CODE,
            expectedAnalyticsEvent = PaymentAnalyticsEvent.Auth3ds1Sdk
        )
        assertThat(threeDs1IntentReturnUrlMap).doesNotContainKey(PAYMENT_INTENT_ID_FOR_3DS1)
    }

    @Test
    fun authenticate_whenRedirectToUrl() {
        verifyAuthenticate(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_REDIRECT,
            expectedUrl = "https://hooks.stripe.com/3d_secure_2_eap/begin_test/src_1Ecaz6CRMbs6FrXfuYKBRSUG/src_client_secret_F6octeOshkgxT47dr0ZxSZiv",
            expectedReturnUrl = "stripe://deeplink",
            expectedRequestCode = PAYMENT_REQUEST_CODE,
            expectedAnalyticsEvent = PaymentAnalyticsEvent.AuthRedirect
        )
    }

    @Test
    fun authenticate_whenRedirectToUrlWithSetupIntent() {
        verifyAuthenticate(
            stripeIntent = SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT,
            expectedUrl = "https://hooks.stripe.com/redirect/authenticate/src_1EqTStGMT9dGPIDGJGPkqE6B?client_secret=src_client_secret_FL9m741mmxtHykDlRTC5aQ02",
            expectedReturnUrl = "stripe://setup_intent_return",
            expectedRequestCode = SETUP_REQUEST_CODE,
            expectedAnalyticsEvent = PaymentAnalyticsEvent.AuthRedirect
        )
    }

    @Test
    fun authenticate_whenDisplayOxxoDetails() {
        verifyAuthenticate(
            stripeIntent = PaymentIntentFixtures.OXXO_REQUIRES_ACTION,
            expectedUrl = "https://payments.stripe.com/oxxo/voucher/test_YWNjdF8xSWN1c1VMMzJLbFJvdDAxLF9KRlBtckVBMERWM0lBZEUyb",
            expectedReturnUrl = null,
            expectedRequestCode = PAYMENT_REQUEST_CODE,
            expectedAnalyticsEvent = null,
            expectedShouldCancelIntentOnUserNavigation = false
        )
    }

    @Test
    fun authenticate_whenRedirectingToCashApp() {
        verifyAuthenticate(
            stripeIntent = PaymentIntentFixtures.CASH_APP_PAY_REQUIRES_ACTION,
            expectedUrl = "https://pm-redirects.stripe.com/authorize/acct_1HvTI7Lu5o3P18Zp/pa_nonce_NC1mezV544wpYmFaXyJpnleeurKO3TZ",
            expectedReturnUrl = "stripesdk://payment_return_url/some_package_name",
            expectedRequestCode = PAYMENT_REQUEST_CODE,
            expectedAnalyticsEvent = null,
            expectedShouldCancelIntentOnUserNavigation = false,
        )
    }

    private fun verifyAuthenticate(
        stripeIntent: StripeIntent,
        expectedUrl: String,
        expectedReturnUrl: String?,
        expectedRequestCode: Int,
        expectedShouldCancelIntentOnUserNavigation: Boolean = true,
        expectedAnalyticsEvent: PaymentAnalyticsEvent?
    ) = runTest {
        authenticator.authenticate(
            host,
            stripeIntent,
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

    private fun verifyAnalytics(event: PaymentAnalyticsEvent) {
        verify(analyticsRequestExecutor)
            .executeAsync(analyticsRequestArgumentCaptor.capture())
        val analyticsRequest = analyticsRequestArgumentCaptor.firstValue
        assertThat(
            analyticsRequest.params.get(AnalyticsFields.EVENT)
        ).isEqualTo(event.toString())
    }

    private companion object {
        private const val ACCOUNT_ID = "acct_123"

        private const val PAYMENT_INTENT_ID_FOR_3DS1 = "pi_1EceMnCRMbs6FrXfCXdF8dnx"
        private const val RETURN_URL_FOR_3DS1 = "stripesdk://payment_return_url"
        private val REQUEST_OPTIONS = ApiRequest.Options(
            apiKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            stripeAccount = ACCOUNT_ID
        )
    }
}
