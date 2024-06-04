package com.stripe.hcaptcha.webview

import android.content.Context
import android.os.Handler
import android.util.Log
import android.view.ViewGroup
import android.view.ViewParent
import android.webkit.WebSettings
import android.webkit.WebView
import com.stripe.hcaptcha.HCaptchaStateListener
import com.stripe.hcaptcha.IHCaptchaVerifier
import com.stripe.hcaptcha.config.HCaptchaConfig
import com.stripe.hcaptcha.config.HCaptchaInternalConfig
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class HCaptchaWebViewHelperTest {

    private val context = mock<Context>()
    private val config = mock<HCaptchaConfig>()
    private val internalConfig = mock<HCaptchaInternalConfig>()
    private val captchaVerifier = mock<IHCaptchaVerifier>()
    private val stateListener = mock<HCaptchaStateListener>()
    private val webView = mock<WebView>()
    private val webSettings = mock<WebSettings>()
    private val handler = mock<Handler>()
    private val htmlProvider = { MOCK_HTML }
    private val androidLogMock = mockStatic(Log::class.java)

    @Before
    fun init() {
        whenever(webView.settings).thenReturn(webSettings)
        whenever(internalConfig.htmlProvider).thenReturn(htmlProvider)
    }

    @After
    fun release() {
        androidLogMock.close()
    }

    @Test
    fun test_constructor() {
        HCaptchaWebViewHelper(handler, context, config, internalConfig, captchaVerifier, stateListener, webView)
        verify(webView).loadDataWithBaseURL(null, MOCK_HTML, "text/html", "UTF-8", null)
        verify(webView, Mockito.times(2))
            .addJavascriptInterface(ArgumentMatchers.any(), ArgumentMatchers.anyString())
    }

    @Test
    fun test_destroy() {
        val webViewHelper =
            HCaptchaWebViewHelper(handler, context, config, internalConfig, captchaVerifier, stateListener, webView)
        val viewParent = mock<ViewGroup>(extraInterfaces = arrayOf(ViewParent::class))
        whenever(webView.parent).thenReturn(viewParent)
        webViewHelper.destroy()
        verify(viewParent).removeView(webView)
        verify(webView, times(2)).removeJavascriptInterface(ArgumentMatchers.anyString())
    }

    @Test
    fun test_destroy_webview_parent_null() {
        val webViewHelper =
            HCaptchaWebViewHelper(handler, context, config, internalConfig, captchaVerifier, stateListener, webView)
        webViewHelper.destroy()
    }

    @Test
    fun test_config_host_pased() {
        val host = "https://my.awesome.host"
        whenever(config.host).thenReturn(host)
        HCaptchaWebViewHelper(handler, context, config, internalConfig, captchaVerifier, stateListener, webView)
        verify(webView).loadDataWithBaseURL(host, MOCK_HTML, "text/html", "UTF-8", null)
    }

    companion object {
        private const val MOCK_HTML = "<html/>"
    }
}
