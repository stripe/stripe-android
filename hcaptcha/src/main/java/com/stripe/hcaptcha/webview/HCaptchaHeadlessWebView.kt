package com.stripe.hcaptcha.webview

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
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
    private val webViewHelper: HCaptchaWebViewHelper
    private var webViewLoaded = false
    private var shouldExecuteOnLoad = false
    private var shouldResetOnLoad = false

    init {
        val webView: WebView = HCaptchaWebView(activity)
        webView.id = R.id.webView
        webView.visibility = View.GONE

        if (webView.parent == null) {
            val rootView = activity.window.decorView.rootView as ViewGroup
            rootView.addView(webView)
        }

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
        if (webViewLoaded) {
            // Safe to execute
            webViewHelper.resetAndExecute()
        } else {
            shouldExecuteOnLoad = true
        }
    }

    override fun onFailure(exception: HCaptchaException) {
        val silentRetry = webViewHelper.shouldRetry(exception)
        if (silentRetry) {
            webViewHelper.resetAndExecute()
        } else {
            listener.onFailure(exception)
        }
    }

    override fun onSuccess(result: String) {
        listener.onSuccess(result)
    }

    override fun onLoaded() {
        webViewLoaded = true
        if (shouldResetOnLoad) {
            shouldResetOnLoad = false
            reset()
        } else if (shouldExecuteOnLoad) {
            shouldExecuteOnLoad = false
            webViewHelper.resetAndExecute()
        }
    }

    override fun onOpen() {
        listener.onOpen()
    }

    override fun reset() {
        if (webViewLoaded) {
            webViewHelper.reset()
            val webView: WebView = webViewHelper.webView
            if (webView.parent != null) {
                (webView.parent as ViewGroup).removeView(webView)
            }
        } else {
            shouldResetOnLoad = true
        }
    }
}
