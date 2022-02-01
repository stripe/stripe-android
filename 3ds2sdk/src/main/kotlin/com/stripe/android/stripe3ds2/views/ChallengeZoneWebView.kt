package com.stripe.android.stripe3ds2.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.VisibleForTesting
import com.stripe.android.stripe3ds2.databinding.StripeChallengeZoneWebViewBinding
import java.util.regex.Pattern

internal class ChallengeZoneWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), FormField {

    val webView: ThreeDS2WebView
    override var userEntry: String = ""
        private set

    internal var onClickListener: OnClickListener? = null

    init {
        val viewBinding = StripeChallengeZoneWebViewBinding.inflate(
            LayoutInflater.from(context),
            this
        )
        webView = viewBinding.webView
        webView.setOnHtmlSubmitListener { data ->
            userEntry = data.orEmpty()
            onClickListener?.onClick(this@ChallengeZoneWebView)
        }
    }

    @VisibleForTesting
    internal fun transformHtml(html: String): String {
        return transformFormActionUrl(transformFormMethod(html))
    }

    private fun transformFormMethod(html: String): String {
        val methodMatcher = PATTERN_METHOD_POST.matcher(html)
        return methodMatcher.replaceAll(METHOD_GET)
    }

    private fun transformFormActionUrl(html: String): String {
        val actionMatcher = PATTERN_FORM_ACTION.matcher(html)
        if (actionMatcher.find()) {
            actionMatcher.group(1)?.let { actionUrl ->
                if (ThreeDS2WebViewClient.CHALLENGE_URL != actionUrl) {
                    return html.replace(actionUrl.toRegex(), ThreeDS2WebViewClient.CHALLENGE_URL)
                }
            }
        }
        return html
    }

    fun loadHtml(html: String?) {
        if (html == null) {
            return
        }

        webView.loadDataWithBaseURL(
            null,
            transformHtml(html),
            HTML_MIME_TYPE,
            ENCODING,
            null
        )
    }

    override fun setOnClickListener(onClickListener: OnClickListener?) {
        this.onClickListener = onClickListener
    }

    private companion object {
        private val PATTERN_METHOD_POST = Pattern.compile(
            "method=\"post\"",
            Pattern.CASE_INSENSITIVE or Pattern.MULTILINE
        )
        private val PATTERN_FORM_ACTION = Pattern.compile(
            "action=\"(.+?)\"",
            Pattern.CASE_INSENSITIVE or Pattern.MULTILINE
        )

        private const val HTML_MIME_TYPE = "text/html"
        private const val ENCODING = "UTF-8"
        private const val METHOD_GET = "method=\"get\""
    }
}
