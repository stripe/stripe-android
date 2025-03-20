package com.stripe.android.connect

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.RestrictTo
import androidx.core.content.withStyledAttributes
import com.stripe.android.connect.webview.StripeConnectWebViewContainer

@PrivateBetaConnectSDK
internal class PayoutsView internal constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    embeddedComponentManager: EmbeddedComponentManager?,
    listener: PayoutsListener?,
    cacheKey: String?,
) :
    StripeComponentView<PayoutsListener, EmptyProps>(
        context = context,
        attrs = attrs,
        defStyleAttr = defStyleAttr,
        embeddedComponent = StripeEmbeddedComponent.PAYOUTS,
        embeddedComponentManager = embeddedComponentManager,
        listener = listener,
        props = EmptyProps,
    ),
    StripeConnectWebViewContainer<PayoutsListener, EmptyProps> {

    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : this(
        context = context,
        attrs = attrs,
        defStyleAttr = defStyleAttr,
        embeddedComponentManager = null,
        listener = null,
        cacheKey = null,
    )

    init {
        var xmlCacheKey: String? = null
        context.withStyledAttributes(attrs, R.styleable.StripeConnectWebViewContainer, defStyleAttr, 0) {
            xmlCacheKey = getString(R.styleable.StripeConnectWebViewContainer_stripeWebViewCacheKey)
        }
        initializeView(cacheKey ?: xmlCacheKey)
    }
}

@PrivateBetaConnectSDK
@RestrictTo(RestrictTo.Scope.LIBRARY)
interface PayoutsListener : StripeEmbeddedComponentListener
