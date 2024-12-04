package com.stripe.android.connect

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.RestrictTo
import com.stripe.android.connect.webview.StripeConnectWebViewContainer
import com.stripe.android.connect.webview.StripeConnectWebViewContainerImpl

@PrivateBetaConnectSDK
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PayoutsView private constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
    webViewContainerBehavior: StripeConnectWebViewContainerImpl<PayoutsListener>,
) : FrameLayout(context, attrs, defStyleAttr),
    StripeConnectWebViewContainer<PayoutsListener> by webViewContainerBehavior {

    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        embeddedComponentManager: EmbeddedComponentManager? = null,
        listener: PayoutsListener? = null,
    ) : this(
        context,
        attrs,
        defStyleAttr,
        StripeConnectWebViewContainerImpl(
            embeddedComponent = StripeEmbeddedComponent.PAYOUTS,
            embeddedComponentManager = embeddedComponentManager,
            listener = listener,
            listenerDelegate = ComponentListenerDelegate.ignore(),
        )
    )

    init {
        webViewContainerBehavior.initializeView(this)
    }
}

@PrivateBetaConnectSDK
interface PayoutsListener : StripeEmbeddedComponentListener
