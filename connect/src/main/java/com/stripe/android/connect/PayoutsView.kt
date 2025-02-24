package com.stripe.android.connect

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.RestrictTo
import androidx.core.content.withStyledAttributes
import com.stripe.android.connect.webview.StripeConnectWebViewContainer
import com.stripe.android.connect.webview.StripeConnectWebViewContainerImpl

@PrivateBetaConnectSDK
class PayoutsView private constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
    cacheKey: String?,
    webViewContainerBehavior: StripeConnectWebViewContainerImpl<PayoutsListener, EmptyProps>,
) : FrameLayout(context, attrs, defStyleAttr),
    StripeConnectWebViewContainer<PayoutsListener, EmptyProps> by webViewContainerBehavior {

    @JvmOverloads
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        embeddedComponentManager: EmbeddedComponentManager? = null,
        listener: PayoutsListener? = null,
        cacheKey: String? = null,
    ) : this(
        context = context,
        attrs = attrs,
        defStyleAttr = defStyleAttr,
        cacheKey = cacheKey,
        webViewContainerBehavior = StripeConnectWebViewContainerImpl(
            embeddedComponent = StripeEmbeddedComponent.PAYOUTS,
            embeddedComponentManager = embeddedComponentManager,
            listener = listener,
            listenerDelegate = ComponentListenerDelegate.ignore(),
            props = EmptyProps,
        )
    )

    init {
        var xmlCacheKey: String? = null
        context.withStyledAttributes(attrs, R.styleable.StripeConnectWebViewContainer, defStyleAttr, 0) {
            xmlCacheKey = getString(R.styleable.StripeConnectWebViewContainer_stripeWebViewCacheKey)
        }
        webViewContainerBehavior.initializeView(this, cacheKey ?: xmlCacheKey)
    }
}

@PrivateBetaConnectSDK
@RestrictTo(RestrictTo.Scope.LIBRARY)
interface PayoutsListener : StripeEmbeddedComponentListener
