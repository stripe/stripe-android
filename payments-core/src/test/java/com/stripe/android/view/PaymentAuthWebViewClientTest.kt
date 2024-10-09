package com.stripe.android.view

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.payments.DefaultReturnUrl
import com.stripe.android.testing.FakeLogger
import com.stripe.android.view.PaymentAuthWebViewClient.Companion.isCompletionUrl
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class PaymentAuthWebViewClientTest {
    private val isPageLoaded = MutableStateFlow(false)

    private val onAuthCompletedErrors = mutableListOf<Throwable>()
    private var activityFinished = false

    private val webView = WebView(ApplicationProvider.getApplicationContext())

    @Test
    fun shouldOverrideUrlLoading_withPaymentIntent_shouldSetResult() {
        val url =
            "stripe://payment_intent_return?payment_intent=pi_123&payment_intent_client_secret=pi_123_secret_456&source_type=card"

        val webViewClient = createWebViewClient(
            "pi_123_secret_456",
            returnUrl = "stripe://payment_intent_return"
        )
        val shouldOverrideUrlLoading = webViewClient.shouldOverrideUrlLoading(
            webView,
            FakeWebResourceRequest(url)
        )
        assertThat(shouldOverrideUrlLoading)
            .isTrue()

        assertThat(onAuthCompletedErrors)
            .isEmpty()
        assertThat(activityFinished)
            .isTrue()
    }

    @Test
    fun shouldOverrideUrlLoading_withSetupIntent_shouldSetResult() {
        val url =
            "stripe://payment_auth?setup_intent=seti_1234&setup_intent_client_secret=seti_1234_secret_5678&source_type=card"
        val webViewClient = createWebViewClient(
            "seti_1234_secret_5678",
            returnUrl = "stripe://payment_auth"
        )

        val shouldOverrideUrlLoading = webViewClient.shouldOverrideUrlLoading(
            webView,
            FakeWebResourceRequest(url)
        )
        assertThat(shouldOverrideUrlLoading)
            .isTrue()

        assertThat(onAuthCompletedErrors)
            .isEmpty()
        assertThat(activityFinished)
            .isTrue()
    }

    @Test
    fun shouldOverrideUrlLoading_withoutReturnUrl_onPaymentIntentImplicitReturnUrl_shouldSetResult() {
        val url =
            "stripe://payment_intent_return?payment_intent=pi_123&payment_intent_client_secret=pi_123_secret_456&source_type=card"
        val webViewClient = createWebViewClient("pi_123_secret_456")

        val shouldOverrideUrlLoading = webViewClient.shouldOverrideUrlLoading(
            webView,
            FakeWebResourceRequest(url)
        )
        assertThat(shouldOverrideUrlLoading)
            .isTrue()

        assertThat(onAuthCompletedErrors)
            .isEmpty()
        assertThat(activityFinished)
            .isTrue()
    }

    @Test
    fun shouldOverrideUrlLoading_withoutReturnUrl_onSetupIntentImplicitReturnUrl_shouldSetResult() {
        val url =
            "stripe://payment_auth?setup_intent=seti_1234&setup_intent_client_secret=seti_1234_secret_5678&source_type=card"
        val webViewClient = createWebViewClient("seti_1234_secret_5678")

        val shouldOverrideUrlLoading = webViewClient.shouldOverrideUrlLoading(
            webView,
            FakeWebResourceRequest(url)
        )
        assertThat(shouldOverrideUrlLoading)
            .isTrue()

        assertThat(onAuthCompletedErrors)
            .isEmpty()
        assertThat(activityFinished)
            .isTrue()
    }

    @Test
    fun shouldOverrideUrlLoading_withoutReturnUrl_shouldNotAutoFinishActivity() {
        val webViewClient = createWebViewClient("pi_123_secret_456")

        val shouldOverrideUrlLoading = webViewClient.shouldOverrideUrlLoading(
            webView,
            FakeWebResourceRequest("https://example.com")
        )
        assertThat(shouldOverrideUrlLoading)
            .isFalse()

        assertThat(onAuthCompletedErrors)
            .isEmpty()
        assertThat(activityFinished)
            .isFalse()
    }

    @Test
    fun shouldOverrideUrlLoading_withKnownReturnUrl_shouldFinish() {
        val url = "stripejs://use_stripe_sdk/return_url"

        val shouldOverrideUrlLoading = createWebViewClient("pi_123_secret_456")
            .shouldOverrideUrlLoading(
                webView,
                FakeWebResourceRequest(url)
            )
        assertThat(shouldOverrideUrlLoading)
            .isTrue()

        assertThat(onAuthCompletedErrors)
            .isEmpty()
        assertThat(activityFinished)
            .isTrue()
    }

    @Test
    fun shouldOverrideUrlLoading_withDefaultReturnUrl_shouldFinish() {
        val url = DefaultReturnUrl.PREFIX + "com.test.package?arg1=1&arg2=2"

        val shouldOverrideUrlLoading = createWebViewClient("pi_123_secret_456")
            .shouldOverrideUrlLoading(
                webView,
                FakeWebResourceRequest(url)
            )
        assertThat(shouldOverrideUrlLoading)
            .isTrue()

        assertThat(onAuthCompletedErrors)
            .isEmpty()
        assertThat(activityFinished)
            .isTrue()
    }

    @Test
    fun onPageFinished_wit3DSecureCompleteUrl_shouldHideProgressAndFinish() {
        val url = "https://hooks.stripe.com/3d_secure/complete/tdsrc_1ExLWoCRMbs6FrXfjPJRYtng"
        createWebViewClient("pi_123_secret_456")
            .onPageFinished(webView, url)
        assertThat(isPageLoaded.value)
            .isTrue()

        assertThat(onAuthCompletedErrors)
            .isEmpty()
        assertThat(activityFinished)
            .isTrue()
    }

    @Test
    fun onPageFinished_witRedirectCompleteUrl_shouldFinish() {
        val url = "https://hooks.stripe.com/redirect/complete/src_1ExLWoCRMbs6FrXfjPJRYtng"
        createWebViewClient("pi_123_secret_456")
            .onPageFinished(webView, url)

        assertThat(onAuthCompletedErrors)
            .isEmpty()
        assertThat(activityFinished)
            .isTrue()
    }

    @Test
    fun shouldOverrideUrlLoading_withOpaqueUri_shouldNotCrash() {
        val url =
            "mailto:patrick@example.com?payment_intent=pi_123&payment_intent_client_secret=pi_123_secret_456&source_type=card"
        val webViewClient = createWebViewClient("pi_123_secret_456")

        val shouldOverrideUrlLoading = webViewClient.shouldOverrideUrlLoading(
            webView,
            FakeWebResourceRequest(url)
        )
        assertThat(shouldOverrideUrlLoading)
            .isTrue()

        assertThat(onAuthCompletedErrors)
            .isEmpty()
    }

    @Test
    fun shouldOverrideUrlLoading_withUnsupportedDeeplink_shouldFinish() {
        val url = "deep://link"
        val webViewClient = createWebViewClient(
            "pi_123_secret_456",
            activityStarter = { throw ActivityNotFoundException() }
        )

        val shouldOverrideUrlLoading = webViewClient.shouldOverrideUrlLoading(
            webView,
            FakeWebResourceRequest(url)
        )
        assertThat(shouldOverrideUrlLoading)
            .isTrue()

        assertThat(onAuthCompletedErrors)
            .hasSize(1)
        assertThat(activityFinished)
            .isTrue()
    }

    @Test
    fun shouldOverloadUrlLoading_withAlipayDeeplink_shouldNotFinishActivity() {
        val url = "alipays://link"
        val webViewClient = createWebViewClient("pi_123_secret_456")

        val shouldOverrideUrlLoading = webViewClient.shouldOverrideUrlLoading(
            webView,
            FakeWebResourceRequest(url)
        )
        assertThat(shouldOverrideUrlLoading)
            .isTrue()

        assertThat(onAuthCompletedErrors)
            .isEmpty()
        assertThat(activityFinished)
            .isFalse()
    }

    @Test
    fun shouldOverrideUrlLoading_withIntentUri_shouldParseUri() {
        val capturedIntents = mutableListOf<Intent>()

        val deepLink =
            "intent://example.com/#Intent;scheme=https;action=android.intent.action.VIEW;end"
        val webViewClient = createWebViewClient(
            "pi_123_secret_456",
            activityStarter = {
                capturedIntents.add(it)
                throw ActivityNotFoundException()
            }
        )

        val shouldOverrideUrlLoading = webViewClient.shouldOverrideUrlLoading(
            webView,
            FakeWebResourceRequest(deepLink)
        )
        assertThat(shouldOverrideUrlLoading)
            .isTrue()

        val intent = capturedIntents.first()
        assertThat(intent.dataString)
            .isEqualTo("https://example.com/")

        assertThat(onAuthCompletedErrors)
            .hasSize(1)
        assertThat(activityFinished)
            .isTrue()
    }

    @Test
    fun shouldOverrideUrlLoading_withAuthenticationUrlWithReturnUrlParam_shouldPopulateCompletionUrl() {
        val url =
            "https://hooks.stripe.com/three_d_secure/authenticate?amount=1250&client_secret=src_client_secret_abc123&return_url=https%3A%2F%2Fhooks.stripe.com%2Fredirect%2Fcomplete%2Fsrc_X9Y8Z7%3Fclient_secret%3Dsrc_client_secret_abc123&source=src_X9Y8Z7&usage=single_use"
        val webViewClient = createWebViewClient("pi_123_secret_456")

        val shouldOverrideUrlLoading = webViewClient.shouldOverrideUrlLoading(
            webView,
            FakeWebResourceRequest(url)
        )
        assertThat(shouldOverrideUrlLoading)
            .isFalse()

        assertThat(webViewClient.completionUrlParam)
            .isEqualTo(
                "https://hooks.stripe.com/redirect/complete/src_X9Y8Z7?client_secret=src_client_secret_abc123"
            )
    }

    @Test
    fun shouldOverrideUrlLoading_withAuthenticationUrlWithoutReturnUrlParam_shouldNotPopulateCompletionUrl() {
        val url =
            "https://hooks.stripe.com/three_d_secure/authenticate?amount=1250&client_secret=src_client_secret_abc123&return_url=&source=src_X9Y8Z7&usage=single_use"
        val webViewClient = createWebViewClient("pi_123_secret_456")

        val shouldOverrideUrlLoading = webViewClient.shouldOverrideUrlLoading(
            webView,
            FakeWebResourceRequest(url)
        )
        assertThat(shouldOverrideUrlLoading)
            .isFalse()

        assertThat(webViewClient.completionUrlParam)
            .isNull()
    }

    @Test
    fun `isCompletionUrl should return expected value for various URLs`() {
        assertThat(
            isCompletionUrl("https://hooks.stripe.com/3d_secure/complete")
        ).isFalse()

        assertThat(
            isCompletionUrl("https://hooks.stripe.com/3d_secure/complete/")
        ).isTrue()

        assertThat(
            isCompletionUrl("https://hooks.stripe.com/redirect/complete/src____123")
        ).isTrue()

        assertThat(
            isCompletionUrl("https://hooks.stripe.com/redirect/complete/src_")
        ).isTrue()

        assertThat(
            isCompletionUrl("https://hooks.stripe.com/redirect/complete/src__")
        ).isTrue()

        assertThat(
            isCompletionUrl("https://hooks.stripe.com/redirect/complete/src_abc123")
        ).isTrue()

        assertThat(
            isCompletionUrl("https://hooks.stripe.com/3d_secure/complete/tdsrc_")
        ).isTrue()

        assertThat(
            isCompletionUrl("https://hooks.stripe.com/3d_secure/complete/tdsrc__")
        ).isTrue()

        assertThat(
            isCompletionUrl("https://hooks.stripe.com/3d_secure/complete/tdsrc_abc123")
        ).isTrue()

        assertThat(
            isCompletionUrl("https://hooks.stripe.com/3d_secure/complete/acct/tdsrc_123")
        ).isTrue()

        assertThat(
            isCompletionUrl("https://hooks.stripe.com/3d_secure/complete/acct_/tdsrc_123")
        ).isTrue()

        assertThat(
            isCompletionUrl("https://hooks.stripe.com/3d_secure/complete/acct_123/tdsrc_")
        ).isTrue()

        assertThat(
            isCompletionUrl("https://hooks.stripe.com/3d_secure/complete/acct_123/tdsrc_456")
        ).isTrue()

        assertThat(
            isCompletionUrl("https://hooks.stripe.com/3d_secure/complete/Xyza1b2C345/tdsrc_456")
        ).isTrue()

        assertThat(
            isCompletionUrl("https://hooks.stripe.com/3d_secure_2/hosted/complete")
        ).isTrue()

        assertThat(
            isCompletionUrl("https://hooks.stripe.com/3d_secure_2/hosted/complete/")
        ).isTrue()
    }

    private fun createWebViewClient(
        clientSecret: String,
        activityStarter: (Intent) -> Unit = {},
        returnUrl: String? = null
    ): PaymentAuthWebViewClient {
        return PaymentAuthWebViewClient(
            FakeLogger(),
            isPageLoaded,
            clientSecret,
            returnUrl,
            activityStarter
        ) { error ->
            error?.let(onAuthCompletedErrors::add)
            activityFinished = true
        }
    }

    private class FakeWebResourceRequest(
        private val url: String
    ) : WebResourceRequest {
        override fun getUrl(): Uri = Uri.parse(url)
        override fun isForMainFrame(): Boolean = false
        override fun isRedirect(): Boolean = false
        override fun hasGesture(): Boolean = false
        override fun getMethod(): String = "GET"
        override fun getRequestHeaders(): Map<String, String> = emptyMap()
    }
}
