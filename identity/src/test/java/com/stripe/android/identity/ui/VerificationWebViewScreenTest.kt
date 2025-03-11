package com.stripe.android.identity.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.identity.IdentityVerificationSheet.VerificationFlowResult
import com.stripe.android.identity.VerificationFlowFinishable
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.viewmodel.IdentityViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowWebView

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class VerificationWebViewScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockVerificationPage = mock<VerificationPage>()
    private val verificationPageLiveData = MutableLiveData<Resource<VerificationPage>>()
    private val mockIdentityViewModel = mock<IdentityViewModel>()
    private val mockVerificationFlowFinishable = mock<VerificationFlowFinishable>()

    private lateinit var webView: WebView
    private lateinit var webViewClient: WebViewClient
    private lateinit var webChromeClient: TestWebChromeClient
    private lateinit var context: Context
    private lateinit var shadowWebView: ShadowWebView

    @Before
    fun setup() {
        // Setup mocks
        whenever(mockVerificationPage.fallbackUrl).thenReturn(FALLBACK_URL)
        whenever(mockIdentityViewModel.verificationPage).thenReturn(verificationPageLiveData)
        whenever(mockIdentityViewModel.errorCause).thenReturn(MutableLiveData())

        // Setup context and WebView
        context = ApplicationProvider.getApplicationContext()
        webView = WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false
                setSupportZoom(false)
            }
        }
        shadowWebView = Shadow.extract(webView)
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                return !request.url.toString().startsWith(FALLBACK_URL)
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                mockVerificationFlowFinishable.finishWithResult(
                    VerificationFlowResult.Failed(RuntimeException(error.description.toString()))
                )
            }

            override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
                if (url.startsWith("$FALLBACK_URL/success")) {
                    mockVerificationFlowFinishable.finishWithResult(VerificationFlowResult.Completed)
                }
            }
        }
        webChromeClient = TestWebChromeClient()
        webView.webViewClient = webViewClient
        webView.webChromeClient = webChromeClient

        composeTestRule.setContent {
            VerificationWebViewScreen(
                identityViewModel = mockIdentityViewModel,
                verificationFlowFinishable = mockVerificationFlowFinishable
            )
        }

        // Post verification page data to trigger WebView setup
        verificationPageLiveData.value = Resource.success(mockVerificationPage)
    }

    @Test
    fun `webview is configured correctly`() {
        with(webView.settings) {
            assertThat(javaScriptEnabled).isTrue()
            assertThat(domStorageEnabled).isTrue()
            assertThat(mediaPlaybackRequiresUserGesture).isFalse()
            assertThat(supportZoom()).isFalse()
        }
    }

    @Test
    fun `camera permission is granted when already allowed`() {
        val mockPermissionRequest = mock<PermissionRequest>()
        whenever(mockPermissionRequest.resources).thenReturn(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))

        // Mock context for permission check
        val mockContext = mock<Context>()
        whenever(mockContext.checkSelfPermission(Manifest.permission.CAMERA))
            .thenReturn(PackageManager.PERMISSION_GRANTED)

        // Simulate permission request
        webChromeClient.onPermissionRequest(mockPermissionRequest)

        verify(mockPermissionRequest).grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
    }

    @Test
    fun `non-verification urls are opened in custom tabs`() {
        val mockRequest = mock<WebResourceRequest>()
        whenever(mockRequest.url).thenReturn(android.net.Uri.parse(EXTERNAL_URL))

        val result = webViewClient.shouldOverrideUrlLoading(webView, mockRequest)

        assertThat(result).isTrue()
    }

    @Test
    fun `verification urls are handled internally`() {
        val mockRequest = mock<WebResourceRequest>()
        whenever(mockRequest.url).thenReturn(android.net.Uri.parse(FALLBACK_URL))

        val result = webViewClient.shouldOverrideUrlLoading(webView, mockRequest)

        assertThat(result).isFalse()
    }

    @Test
    fun `webview error triggers failure result`() {
        val mockError = mock<WebResourceError>()
        whenever(mockError.description).thenReturn(ERROR_DESCRIPTION)

        webViewClient.onReceivedError(
            webView,
            mock(),
            mockError
        )

        val captor = argumentCaptor<VerificationFlowResult.Failed>()
        verify(mockVerificationFlowFinishable).finishWithResult(captor.capture())
        assertThat(captor.firstValue.throwable.message).contains(ERROR_DESCRIPTION)
    }

    @Test
    fun `success url triggers completed result`() {
        webViewClient.doUpdateVisitedHistory(
            webView,
            "$FALLBACK_URL/success",
            false
        )

        verify(mockVerificationFlowFinishable).finishWithResult(VerificationFlowResult.Completed)
    }

    private class TestWebChromeClient : WebChromeClient() {
        override fun onPermissionRequest(request: PermissionRequest) {
            if (request.resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
                request.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
            } else {
                request.deny()
            }
        }
    }

    private companion object {
        const val FALLBACK_URL = "https://verify.stripe.com/start/test"
        const val EXTERNAL_URL = "https://stripe.com"
        const val ERROR_DESCRIPTION = "Failed to load resource"
    }
} 