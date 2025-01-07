package com.stripe.android.connect

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.RestrictTo
import com.stripe.android.connect.webview.StripeConnectWebViewContainer
import com.stripe.android.connect.webview.StripeConnectWebViewContainerImpl

@PrivateBetaConnectSDK
class PayoutsView private constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
    private val webViewContainerBehavior: StripeConnectWebViewContainerImpl<PayoutsListener, EmptyProps>,
) : FrameLayout(context, attrs, defStyleAttr),
    StripeConnectWebViewContainer<PayoutsListener, EmptyProps> by webViewContainerBehavior {

    init {
        webViewContainerBehavior.initializeView(this)
    }

    @JvmOverloads
    @RestrictTo(RestrictTo.Scope.LIBRARY)
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
            props = EmptyProps,
        )
    ) {
        if (embeddedComponentManager != null) {
            webViewContainerBehavior.initializeInternal(
                embeddedComponentManager = embeddedComponentManager,
                listener = listener,
                propsJson = EmptyProps.toJsonObject(),
            )
        }
    }
}

@PrivateBetaConnectSDK
@RestrictTo(RestrictTo.Scope.LIBRARY)
interface PayoutsListener : StripeEmbeddedComponentListener
