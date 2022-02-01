package com.stripe.android.stripe3ds2.views

import android.content.Context
import android.util.AttributeSet
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

internal class ThreeDS2WebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    private val webViewClient: ThreeDS2WebViewClient = ThreeDS2WebViewClient()

    init {
        configureSettings()
        super.setWebViewClient(webViewClient)
    }

    override fun setWebViewClient(client: WebViewClient) {}

    internal fun setOnHtmlSubmitListener(listener: ThreeDS2WebViewClient.OnHtmlSubmitListener?) {
        webViewClient.listener = listener
    }

    private fun configureSettings() {
        settings.apply {
            cacheMode = WebSettings.LOAD_NO_CACHE
            javaScriptEnabled = false
            allowContentAccess = false
            blockNetworkImage = true
            blockNetworkLoads = true

//            //works
//            javaScriptEnabled = true
//            allowContentAccess = true
//            blockNetworkImage = false
//            blockNetworkLoads = false

//            // does not work
//            javaScriptEnabled = false
//            allowContentAccess = true
//            blockNetworkImage = false
//            blockNetworkLoads = false

//            // mostly works except for images
//            javaScriptEnabled = true
//            allowContentAccess = false
//            blockNetworkImage = true
//            blockNetworkLoads = true

//            // Works with images
//            javaScriptEnabled = true // changed from false
//            allowContentAccess = false
//            blockNetworkImage = false  // changed from true
//            blockNetworkLoads = true
        }
    }
}
