package com.stripe.android.connect.webview

import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.RestrictTo
import com.stripe.android.connect.EmbeddedComponentManager
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.StripeEmbeddedComponent
import com.stripe.android.connect.databinding.StripeConnectWebviewBinding

@PrivateBetaConnectSDK
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class StripeConnectWebViewContainerBehavior(
    val component: StripeEmbeddedComponent,
    val embeddedComponentManager: EmbeddedComponentManager? = null,
) : StripeConnectWebViewContainer {

    var stripeWebViewClient: StripeConnectWebViewClient? = null
        private set

    lateinit var viewBinding: StripeConnectWebviewBinding
        private set

    fun initialize(view: FrameLayout) {
        viewBinding = StripeConnectWebviewBinding.inflate(LayoutInflater.from(view.context), view)

        embeddedComponentManager?.let {
            configureWebView(it)
        }
    }

    override fun setEmbeddedComponentManager(embeddedComponentManager: EmbeddedComponentManager) {
        if (stripeWebViewClient != null) {
            throw IllegalStateException("EmbeddedComponentManager already set")
        }
        configureWebView(embeddedComponentManager)
    }

    private fun configureWebView(embeddedComponentManager: EmbeddedComponentManager) {
        stripeWebViewClient = StripeConnectWebViewClient(
            embeddedComponentManager,
            component,
        ).apply {
            configureAndLoadWebView(viewBinding.stripeWebView)
        }
    }
}
