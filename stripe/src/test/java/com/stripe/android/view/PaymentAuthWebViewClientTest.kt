package com.stripe.android.view

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.webkit.WebView
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.FakeLogger
import com.stripe.android.PaymentConfiguration
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class PaymentAuthWebViewClientTest {
    private val isPageLoaded = MutableLiveData(false)

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val activityScenarioFactory = ActivityScenarioFactory(context)
    private val webView: WebView by lazy {
        activityScenarioFactory.createView { WebView(it) }
    }

    private val authCompleteErrors = mutableListOf<Throwable>()
    private var onAuthCompleted = false
    private val activityFinisher = { error: Throwable? ->
        error?.let(authCompleteErrors::add)
        onAuthCompleted = true
    }

    @BeforeTest
    fun setup() {
        PaymentConfiguration.init(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
    }

    @Test
    fun `shouldOverrideUrlLoading with PaymentIntent should invoke onAuthCompleted() without errors`() {
        val url =
            "stripe://payment_intent_return?payment_intent=pi_123&payment_intent_client_secret=pi_123_secret_456&source_type=card"
        val paymentAuthWebViewClient = createWebViewClient(
            "pi_123_secret_456",
            returnUrl = "stripe://payment_intent_return"
        )
        paymentAuthWebViewClient.shouldOverrideUrlLoading(webView, url)

        assertThat(authCompleteErrors)
            .isEmpty()
        assertThat(onAuthCompleted)
            .isTrue()
    }

    @Test
    fun `shouldOverrideUrlLoading with SetupIntent should invoke onAuthCompleted() without errors`() {
        val url =
            "stripe://payment_auth?setup_intent=seti_1234&setup_intent_client_secret=seti_1234_secret_5678&source_type=card"
        val paymentAuthWebViewClient = createWebViewClient(
            "seti_1234_secret_5678",
            returnUrl = "stripe://payment_auth"
        )
        paymentAuthWebViewClient.shouldOverrideUrlLoading(webView, url)

        assertThat(authCompleteErrors)
            .isEmpty()
        assertThat(onAuthCompleted)
            .isTrue()
    }

    @Test
    fun `shouldOverrideUrlLoading without return URL with PaymentIntent implicit return URL should invoke onAuthCompleted() without errors`() {
        val url =
            "stripe://payment_intent_return?payment_intent=pi_123&payment_intent_client_secret=pi_123_secret_456&source_type=card"
        val paymentAuthWebViewClient = createWebViewClient("pi_123_secret_456")
        paymentAuthWebViewClient.shouldOverrideUrlLoading(webView, url)

        assertThat(authCompleteErrors)
            .isEmpty()
        assertThat(onAuthCompleted)
            .isTrue()
    }

    @Test
    fun `shouldOverrideUrlLoading without return URL with SetupIntent implicit return URL should invoke onAuthCompleted() without errors`() {
        val url =
            "stripe://payment_auth?setup_intent=seti_1234&setup_intent_client_secret=seti_1234_secret_5678&source_type=card"
        val paymentAuthWebViewClient = createWebViewClient("seti_1234_secret_5678")
        paymentAuthWebViewClient.shouldOverrideUrlLoading(webView, url)

        assertThat(authCompleteErrors)
            .isEmpty()
        assertThat(onAuthCompleted)
            .isTrue()
    }

    @Test
    fun shouldOverrideUrlLoading_withoutReturnUrl_shouldNotInvokeOnAuthCompleted() {
        val paymentAuthWebViewClient = createWebViewClient("pi_123_secret_456")
        paymentAuthWebViewClient.shouldOverrideUrlLoading(webView, "https://example.com")

        assertThat(onAuthCompleted)
            .isFalse()
    }

    @Test
    fun `shouldOverrideUrlLoading with known return URL should invoke onAuthCompleted()`() {
        val paymentAuthWebViewClient = createWebViewClient("pi_123_secret_456")
        paymentAuthWebViewClient.shouldOverrideUrlLoading(
            webView,
            "stripejs://use_stripe_sdk/return_url"
        )

        assertThat(onAuthCompleted)
            .isTrue()
    }

    @Test
    fun `onPageFinished with 3DS complete URL should set isPageLoaded=true and invoke onAuthCompleted()`() {
        val paymentAuthWebViewClient = createWebViewClient("pi_123_secret_456")
        paymentAuthWebViewClient.onPageFinished(
            webView,
            "https://hooks.stripe.com/3d_secure/complete/tdsrc_1ExLWoCRMbs6FrXfjPJRYtng"
        )
        assertThat(isPageLoaded.value)
            .isTrue()
        assertThat(onAuthCompleted)
            .isTrue()
    }

    @Test
    fun `onPageFinished with redirect complete URL invoke onAuthCompleted()`() {
        val paymentAuthWebViewClient = createWebViewClient("pi_123_secret_456")
        paymentAuthWebViewClient.onPageFinished(
            webView,
            "https://hooks.stripe.com/redirect/complete/src_1ExLWoCRMbs6FrXfjPJRYtng"
        )
        assertThat(onAuthCompleted)
            .isTrue()
    }

    @Test
    fun shouldOverrideUrlLoading_withOpaqueUri_shouldNotCrash() {
        val url =
            "mailto:patrick@example.com?payment_intent=pi_123&payment_intent_client_secret=pi_123_secret_456&source_type=card"
        val paymentAuthWebViewClient = createWebViewClient("pi_123_secret_456")
        paymentAuthWebViewClient.shouldOverrideUrlLoading(webView, url)
        assertThat(onAuthCompleted)
            .isFalse()
    }

    @Test
    fun shouldOverrideUrlLoading_withUnsupportedDeeplink_shouldFinish() {
        val url = "deep://link"
        val paymentAuthWebViewClient = createWebViewClient(
            "pi_123_secret_456",
            activityStarter = { throw ActivityNotFoundException() }
        )
        paymentAuthWebViewClient.shouldOverrideUrlLoading(webView, url)
        assertThat(onAuthCompleted)
            .isTrue()
        assertThat(authCompleteErrors)
            .hasSize(1)
    }

    @Test
    fun `shouldOverloadUrlLoading withAlipayDeeplink should not invoke onAuthComplete() with error`() {
        val url = "alipays://link"
        val paymentAuthWebViewClient = createWebViewClient(
            "pi_123_secret_456",
            activityStarter = { throw ActivityNotFoundException() }
        )
        paymentAuthWebViewClient.shouldOverrideUrlLoading(webView, url)

        assertThat(onAuthCompleted)
            .isFalse()
        assertThat(authCompleteErrors)
            .isEmpty()
    }

    @Test
    fun shouldOverloadUrlLoading_withAlipayDeeplink_shouldNotFinishActivity() {
        val url = "alipays://link"
        val paymentAuthWebViewClient = createWebViewClient("pi_123_secret_456")
        paymentAuthWebViewClient.shouldOverrideUrlLoading(webView, url)
        assertThat(onAuthCompleted)
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
        paymentAuthWebViewClient.shouldOverrideUrlLoading(webView, deepLink)

        val intent = capturedIntents.first()
        assertThat(intent.dataString)
            .isEqualTo("https://example.com/")

        assertThat(onAuthCompleted)
            .isTrue()
        assertThat(authCompleteErrors)
            .hasSize(1)
    }

    @Test
    fun shouldOverrideUrlLoading_withAuthenticationUrlWithReturnUrlParam_shouldPopulateCompletionUrl() {
        val url =
            "https://hooks.stripe.com/three_d_secure/authenticate?amount=1250&client_secret=src_client_secret_abc123&return_url=https%3A%2F%2Fhooks.stripe.com%2Fredirect%2Fcomplete%2Fsrc_X9Y8Z7%3Fclient_secret%3Dsrc_client_secret_abc123&source=src_X9Y8Z7&usage=single_use"
        val paymentAuthWebViewClient = createWebViewClient("pi_123_secret_456")
        paymentAuthWebViewClient.shouldOverrideUrlLoading(webView, url)
        assertThat(paymentAuthWebViewClient.completionUrlParam)
            .isEqualTo(
                "https://hooks.stripe.com/redirect/complete/src_X9Y8Z7?client_secret=src_client_secret_abc123"
            )

        assertThat(onAuthCompleted)
            .isFalse()
    }

    @Test
    fun shouldOverrideUrlLoading_withAuthenticationUrlWithoutReturnUrlParam_shouldNotPopulateCompletionUrl() {
        val url =
            "https://hooks.stripe.com/three_d_secure/authenticate?amount=1250&client_secret=src_client_secret_abc123&return_url=&source=src_X9Y8Z7&usage=single_use"
        val paymentAuthWebViewClient = createWebViewClient("pi_123_secret_456")
        paymentAuthWebViewClient.shouldOverrideUrlLoading(webView, url)
        assertThat(paymentAuthWebViewClient.completionUrlParam)
            .isNull()

        assertThat(onAuthCompleted)
            .isFalse()
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
