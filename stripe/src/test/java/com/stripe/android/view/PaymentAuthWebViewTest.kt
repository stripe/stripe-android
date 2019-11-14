package com.stripe.android.view

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.webkit.WebView
import android.widget.ProgressBar
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import com.stripe.android.FakeLogger
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentAuthWebViewTest {

    @Mock
    private lateinit var activity: Activity
    @Mock
    private lateinit var progressBar: ProgressBar
    @Mock
    private lateinit var webView: WebView
    @Mock
    private lateinit var packageManager: PackageManager

    private lateinit var intentArgumentCaptor: KArgumentCaptor<Intent>

    @BeforeTest
    fun setup() {
        MockitoAnnotations.initMocks(this)
        intentArgumentCaptor = argumentCaptor()
    }

    @Test
    fun shouldOverrideUrlLoading_withPaymentIntent_shouldSetResult() {
        val url = "stripe://payment_intent_return?payment_intent=pi_123&" + "payment_intent_client_secret=pi_123_secret_456&source_type=card"
        val paymentAuthWebViewClient = createWebViewClient(
            "pi_123_secret_456",
            "stripe://payment_intent_return"
        )
        paymentAuthWebViewClient.shouldOverrideUrlLoading(webView, url)
        verify(activity).finish()
    }

    @Test
    fun shouldOverrideUrlLoading_withSetupIntent_shouldSetResult() {
        val url = "stripe://payment_auth?setup_intent=seti_1234" + "&setup_intent_client_secret=seti_1234_secret_5678&source_type=card"
        val paymentAuthWebViewClient = createWebViewClient("seti_1234_secret_5678", "stripe://payment_auth")
        paymentAuthWebViewClient.shouldOverrideUrlLoading(webView, url)
        verify(activity).finish()
    }

    @Test
    fun shouldOverrideUrlLoading_withoutReturnUrl_onPaymentIntentImplicitReturnUrl_shouldSetResult() {
        val url = "stripe://payment_intent_return?payment_intent=pi_123&" + "payment_intent_client_secret=pi_123_secret_456&source_type=card"
        val paymentAuthWebViewClient = createWebViewClient("pi_123_secret_456")
        paymentAuthWebViewClient.shouldOverrideUrlLoading(webView, url)
        verify(activity).finish()
    }

    @Test
    fun shouldOverrideUrlLoading_withoutReturnUrl_onSetupIntentImplicitReturnUrl_shouldSetResult() {
        val url = "stripe://payment_auth?setup_intent=seti_1234" + "&setup_intent_client_secret=seti_1234_secret_5678&source_type=card"
        val paymentAuthWebViewClient = createWebViewClient("seti_1234_secret_5678")
        paymentAuthWebViewClient.shouldOverrideUrlLoading(webView, url)
        verify(activity).finish()
    }

    @Test
    fun shouldOverrideUrlLoading_withoutReturnUrl_shouldNotAutoFinishActivity() {
        val paymentAuthWebViewClient = createWebViewClient("pi_123_secret_456")
        paymentAuthWebViewClient.shouldOverrideUrlLoading(webView,
            "https://example.com")
        verify(activity, never()).finish()
    }

    @Test
    fun shouldOverrideUrlLoading_withKnownReturnUrl_shouldFinish() {
        val paymentAuthWebViewClient = createWebViewClient("pi_123_secret_456")
        paymentAuthWebViewClient.shouldOverrideUrlLoading(webView,
            "stripejs://use_stripe_sdk/return_url")
        verify(activity).finish()
    }

    @Test
    fun onPageFinished_wit3DSecureCompleteUrl_shouldFinish() {
        val paymentAuthWebViewClient = createWebViewClient("pi_123_secret_456")
        paymentAuthWebViewClient.onPageFinished(webView,
            "https://hooks.stripe.com/3d_secure/complete/tdsrc_1ExLWoCRMbs6FrXfjPJRYtng")
        verify(activity).finish()
    }

    @Test
    fun onPageFinished_witRedirectCompleteUrl_shouldFinish() {
        val paymentAuthWebViewClient = createWebViewClient("pi_123_secret_456")
        paymentAuthWebViewClient.onPageFinished(webView,
            "https://hooks.stripe.com/redirect/complete/src_1ExLWoCRMbs6FrXfjPJRYtng")
        verify(activity).finish()
    }

    @Test
    fun shouldOverrideUrlLoading_withOpaqueUri_shouldNotCrash() {
        val url = "mailto:patrick@example.com?payment_intent=pi_123&" + "payment_intent_client_secret=pi_123_secret_456&source_type=card"
        val paymentAuthWebViewClient = createWebViewClient("pi_123_secret_456")
        paymentAuthWebViewClient.shouldOverrideUrlLoading(webView, url)
    }

    @Test
    fun shouldOverrideUrlLoading_withUnsupportedDeeplink_shouldFinish() {
        val url = "deep://link"
        val paymentAuthWebViewClient = createWebViewClient("pi_123_secret_456")
        paymentAuthWebViewClient.shouldOverrideUrlLoading(webView, url)
        verify(activity).finish()
    }

    @Test
    fun shouldOverrideUrlLoading_withIntentUri_shouldParseUri() {
        val deepLink = "intent://example.com/#Intent;scheme=https;action=android.intent.action.VIEW;end"
        val paymentAuthWebViewClient = createWebViewClient("pi_123_secret_456")
        paymentAuthWebViewClient.shouldOverrideUrlLoading(webView, deepLink)
        verify(packageManager).resolveActivity(
            intentArgumentCaptor.capture(),
            eq(PackageManager.MATCH_DEFAULT_ONLY)
        )
        val intent = intentArgumentCaptor.firstValue
        assertEquals("https://example.com/", intent.dataString)
        verify(activity).finish()
    }

    @Test
    fun shouldOverrideUrlLoading_withAuthenticationUrlWithReturnUrlParam_shouldPopulateCompletionUrl() {
        val url = "https://hooks.stripe.com/three_d_secure/authenticate?amount=1250&client_secret=src_client_secret_abc123&return_url=https%3A%2F%2Fhooks.stripe.com%2Fredirect%2Fcomplete%2Fsrc_X9Y8Z7%3Fclient_secret%3Dsrc_client_secret_abc123&source=src_X9Y8Z7&usage=single_use"
        val paymentAuthWebViewClient = createWebViewClient("pi_123_secret_456")
        paymentAuthWebViewClient.shouldOverrideUrlLoading(webView, url)
        assertEquals(
            "https://hooks.stripe.com/redirect/complete/src_X9Y8Z7?client_secret=src_client_secret_abc123",
            paymentAuthWebViewClient.completionUrlParam
        )
    }

    @Test
    fun shouldOverrideUrlLoading_withAuthenticationUrlWithoutReturnUrlParam_shouldNotPopulateCompletionUrl() {
        val url = "https://hooks.stripe.com/three_d_secure/authenticate?amount=1250&client_secret=src_client_secret_abc123&return_url=&source=src_X9Y8Z7&usage=single_use"
        val paymentAuthWebViewClient = createWebViewClient("pi_123_secret_456")
        paymentAuthWebViewClient.shouldOverrideUrlLoading(webView, url)
        assertNull(paymentAuthWebViewClient.completionUrlParam)
    }

    private fun createWebViewClient(
        clientSecret: String,
        returnUrl: String? = null
    ): PaymentAuthWebView.PaymentAuthWebViewClient {
        return PaymentAuthWebView.PaymentAuthWebViewClient(
            activity,
            packageManager,
            FakeLogger(),
            progressBar,
            clientSecret,
            returnUrl
        )
    }
}
