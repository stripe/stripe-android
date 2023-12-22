package com.stripe.hcaptcha.webview

import android.content.Context
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.ContentInfo
import android.webkit.WebView
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class HCaptchaWebView : WebView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes
    )

    /**
     * Workaround for crash in WebViewChromium
     * Details:
     * - [stackoverflow](https://stackoverflow.com/questions/58519749)
     * - [github issues](https://github.com/zulip/zulip-mobile/issues/4051#issuecomment-616855833)
     */
    override fun onCheckIsTextEditor(): Boolean {
        return if (Looper.myLooper() == Looper.getMainLooper()) {
            super.onCheckIsTextEditor()
        } else {
            false
        }
    }

    override fun performClick(): Boolean {
        return false
    }

    override fun onReceiveContent(payload: ContentInfo): ContentInfo? {
        Log.d("TEST", "got content")
        return super.onReceiveContent(payload)
    }
}
