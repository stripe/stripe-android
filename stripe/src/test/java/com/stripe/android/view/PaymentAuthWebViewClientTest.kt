package com.stripe.android.view

import android.content.ActivityNotFoundException
import android.content.Intent
import android.webkit.WebView
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.FakeLogger
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class PaymentAuthWebViewClientTest {

    private val isPageLoaded = MutableLiveData(false)

    private var activityFinished = false
    private val activityFinisher: () -> Unit = {
        activityFinished = true
    }

    private val webView = WebView(ApplicationProvider.getApplicationContext())

    @Test
    fun shouldOverrideUrlLoading_withPaymentIntent_shouldSetResult() {
        val url =
            "stripe://payment_intent_return?payment_intent=pi_123&" + "payment_intent_client_secret=pi_123_secret_456&source_type=card"
        val paymentAuthWebViewClient = createWebViewClient(
            "pi_123_secret_456",
            returnUrl = "stripe://payment_intent_return"
        )

        val shouldOverrideUrlLoading =
            paymentAuthWebViewClient.shouldOverrideUrlLoading(webView, url)
        assertThat(shouldOverrideUrlLoading)
            .isTrue()

        assertThat(activityFinished)
            .isTrue()
    }

    @Test
    fun shouldOverrideUrlLoading_withSetupIntent_shouldSetResult() {
        val url =
            "stripe://payment_auth?setup_intent=seti_1234" + "&setup_intent_client_secret=seti_1234_secret_5678&source_type=card"
        val paymentAuthWebViewClient = createWebViewClient(
            "seti_1234_secret_5678",
            returnUrl = "stripe://payment_auth"
        )

        val shouldOverrideUrlLoading = paymentAuthWebViewClient.shouldOverrideUrlLoading(
            webView,
            url
        )
        assertThat(shouldOverrideUrlLoading)
            .isTrue()

        assertThat(activityFinished)
            .isTrue()
    }

    @Test
    fun shouldOverrideUrlLoading_withoutReturnUrl_onPaymentIntentImplicitReturnUrl_shouldSetResult() {
        val url =
            "stripe://payment_intent_return?payment_intent=pi_123&" + "payment_intent_client_secret=pi_123_secret_456&source_type=card"
        val paymentAuthWebViewClient = createWebViewClient("pi_123_secret_456")

        val shouldOverrideUrlLoading = paymentAuthWebViewClient.shouldOverrideUrlLoading(
            webView,
            url
        )
        assertThat(shouldOverrideUrlLoading)
            .isTrue()

        assertThat(activityFinished)
            .isTrue()
    }

    @Test
    fun shouldOverrideUrlLoading_withoutReturnUrl_onSetupIntentImplicitReturnUrl_shouldSetResult() {
        val url =
            "stripe://payment_auth?setup_intent=seti_1234" + "&setup_intent_client_secret=seti_1234_secret_5678&source_type=card"
        val paymentAuthWebViewClient = createWebViewClient("seti_1234_secret_5678")

        val shouldOverrideUrlLoading = paymentAuthWebViewClient.shouldOverrideUrlLoading(
            webView,
            url
        )
        assertThat(shouldOverrideUrlLoading)
            .isTrue()

        assertThat(activityFinished)
            .isTrue()
    }

    @Test
    fun shouldOverrideUrlLoading_withoutReturnUrl_shouldNotAutoFinishActivity() {
        val paymentAuthWebViewClient = createWebViewClient("pi_123_secret_456")

        val shouldOverrideUrlLoading = paymentAuthWebViewClient.shouldOverrideUrlLoading(
            webView,
            "https://example.com"
        )
        assertThat(shouldOverrideUrlLoading)
            .isFalse()

        assertThat(activityFinished)
            .isFalse()
    }

    @Test
    fun shouldOverrideUrlLoading_withKnownReturnUrl_shouldFinish() {
        val paymentAuthWebViewClient = createWebViewClient("pi_123_secret_456")

        val shouldOverrideUrlLoading = paymentAuthWebViewClient.shouldOverrideUrlLoading(
            webView,
            "stripejs://use_stripe_sdk/return_url"
        )
        assertThat(shouldOverrideUrlLoading)
            .isTrue()

        assertThat(activityFinished)
            .isTrue()
    }

    @Test
    fun onPageFinished_wit3DSecureCompleteUrl_shouldHideProgressAndFinish() {
        val paymentAuthWebViewClient = createWebViewClient("pi_123_secret_456")
        paymentAuthWebViewClient.onPageFinished(
            webView,
            "https://hooks.stripe.com/3d_secure/complete/tdsrc_1ExLWoCRMbs6FrXfjPJRYtng"
        )
        assertThat(isPageLoaded.value)
            .isTrue()
        assertThat(activityFinished)
            .isTrue()
    }

    @Test
    fun onPageFinished_witRedirectCompleteUrl_shouldFinish() {
        val paymentAuthWebViewClient = createWebViewClient("pi_123_secret_456")
        paymentAuthWebViewClient.onPageFinished(
            webView,
            "https://hooks.stripe.com/redirect/complete/src_1ExLWoCRMbs6FrXfjPJRYtng"
        )
        assertThat(activityFinished)
            .isTrue()
    }

    @Test
    fun shouldOverrideUrlLoading_withOpaqueUri_shouldNotCrash() {
        val url =
            "mailto:patrick@example.com?payment_intent=pi_123&" + "payment_intent_client_secret=pi_123_secret_456&source_type=card"
        val paymentAuthWebViewClient = createWebViewClient("pi_123_secret_456")

        val shouldOverrideUrlLoading =
            paymentAuthWebViewClient.shouldOverrideUrlLoading(webView, url)
        assertThat(shouldOverrideUrlLoading)
            .isTrue()
    }

    @Test
    fun shouldOverrideUrlLoading_withUnsupportedDeeplink_shouldFinish() {
        val url = "deep://link"
        val paymentAuthWebViewClient = createWebViewClient(
            "pi_123_secret_456",
            activityStarter = { throw ActivityNotFoundException() }
        )

        val shouldOverrideUrlLoading =
            paymentAuthWebViewClient.shouldOverrideUrlLoading(webView, url)
        assertThat(shouldOverrideUrlLoading)
            .isTrue()

        assertThat(activityFinished)
            .isTrue()
    }

    @Test
    fun shouldOverloadUrlLoading_withAlipayDeeplink_shouldNotFinishActivity() {
        val url = "alipays://link"
        val paymentAuthWebViewClient = createWebViewClient("pi_123_secret_456")

        val shouldOverrideUrlLoading =
            paymentAuthWebViewClient.shouldOverrideUrlLoading(webView, url)
        assertThat(shouldOverrideUrlLoading)
            .isTrue()

        assertThat(activityFinished)
            .isFalse()
    }

    @Test
    fun shouldOverrideUrlLoading_withIntentUri_shouldParseUri() {
        val capturedIntents = mutableListOf<Intent>()

        val deepLink =
            "intent://example.com/#Intent;scheme=https;action=android.intent.action.VIEW;end"
        val paymentAuthWebViewClient = createWebViewClient(
            "pi_123_secret_456",
            activityStarter = {
                capturedIntents.add(it)
                throw ActivityNotFoundException()
            }
        )

        val shouldOverrideUrlLoading = paymentAuthWebViewClient.shouldOverrideUrlLoading(
            webView,
            deepLink
        )
        assertThat(shouldOverrideUrlLoading)
            .isTrue()

        val intent = capturedIntents.first()
        assertThat(intent.dataString)
            .isEqualTo("https://example.com/")
        assertThat(activityFinished)
            .isTrue()
    }

    @Test
    fun shouldOverrideUrlLoading_withAuthenticationUrlWithReturnUrlParam_shouldPopulateCompletionUrl() {
        val url =
            "https://hooks.stripe.com/three_d_secure/authenticate?amount=1250&client_secret=src_client_secret_abc123&return_url=https%3A%2F%2Fhooks.stripe.com%2Fredirect%2Fcomplete%2Fsrc_X9Y8Z7%3Fclient_secret%3Dsrc_client_secret_abc123&source=src_X9Y8Z7&usage=single_use"
        val paymentAuthWebViewClient = createWebViewClient("pi_123_secret_456")

        val shouldOverrideUrlLoading = paymentAuthWebViewClient.shouldOverrideUrlLoading(
            webView,
            url
        )
        assertThat(shouldOverrideUrlLoading)
            .isFalse()

        assertThat(paymentAuthWebViewClient.completionUrlParam)
            .isEqualTo(
                "https://hooks.stripe.com/redirect/complete/src_X9Y8Z7?client_secret=src_client_secret_abc123"
            )
    }

    @Test
    fun shouldOverrideUrlLoading_withAuthenticationUrlWithoutReturnUrlParam_shouldNotPopulateCompletionUrl() {
        val url =
            "https://hooks.stripe.com/three_d_secure/authenticate?amount=1250&client_secret=src_client_secret_abc123&return_url=&source=src_X9Y8Z7&usage=single_use"
        val paymentAuthWebViewClient = createWebViewClient("pi_123_secret_456")

        val shouldOverrideUrlLoading = paymentAuthWebViewClient.shouldOverrideUrlLoading(
            webView,
            url
        )
        assertThat(shouldOverrideUrlLoading)
            .isFalse()

        assertThat(paymentAuthWebViewClient.completionUrlParam)
            .isNull()
    }

    private fun createWebViewClient(
        clientSecret: String,
        activityStarter: (Intent) -> Unit = {},
        returnUrl: String? = null
    ): PaymentAuthWebViewClient {
        return PaymentAuthWebViewClient(
            activityStarter,
            activityFinisher,
            FakeLogger(),
            isPageLoaded,
            clientSecret,
            returnUrl
        )
    }
}
