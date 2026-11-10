package com.stripe.hcaptcha.webview

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.WebView
import androidx.annotation.RestrictTo
import androidx.fragment.app.FragmentActivity
import com.stripe.hcaptcha.HCaptchaException
import com.stripe.hcaptcha.HCaptchaStateListener
import com.stripe.hcaptcha.IHCaptchaVerifier
import com.stripe.hcaptcha.R
import com.stripe.hcaptcha.config.HCaptchaConfig
import com.stripe.hcaptcha.config.HCaptchaInternalConfig

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class HCaptchaHeadlessWebView(
    activity: Activity,
    config: HCaptchaConfig,
    internalConfig: HCaptchaInternalConfig,
    private val listener: HCaptchaStateListener
) : IHCaptchaVerifier {
    internal val webViewHelper: HCaptchaWebViewHelper
    private var webViewLoaded = false
    private var destroyed = false
    private var shouldExecuteOnLoad = false

    init {
        val webView: WebView = HCaptchaWebView(activity)
        webView.id = R.id.webView
        webView.visibility = View.GONE

        webViewHelper = HCaptchaWebViewHelper(
            Handler(Looper.getMainLooper()),
            activity,
            config,
            internalConfig,
            this,
            listener,
            webView
        )
    }

    override fun startVerification(activity: FragmentActivity) {
        if (destroyed) {
            return
        }
        if (webViewLoaded) {
            // Safe to execute
            webViewHelper.resetAndExecute()
        } else {
            shouldExecuteOnLoad = true
        }
    }

    override fun onFailure(exception: HCaptchaException) {
        if (destroyed) {
            return
        }
        val silentRetry = webViewHelper.shouldRetry(exception)
        if (silentRetry) {
            webViewHelper.resetAndExecute()
        } else {
            listener.onFailure(exception)
        }
    }

    override fun onSuccess(result: String) {
        if (destroyed) {
            return
        }
        listener.onSuccess(result)
    }

    override fun onLoaded() {
        if (destroyed) {
            return
        }
        webViewLoaded = true
        if (shouldExecuteOnLoad) {
            shouldExecuteOnLoad = false
            webViewHelper.resetAndExecute()
        }
    }

    override fun onOpen() {
        if (destroyed) {
            return
        }
        listener.onOpen()
    }

    override fun reset() {
        if (destroyed) {
            return
        }
        destroyWebView()
    }

    private fun destroyWebView() {
        destroyed = true
        webViewLoaded = false
        shouldExecuteOnLoad = false
        webViewHelper.destroy()
    }
}
