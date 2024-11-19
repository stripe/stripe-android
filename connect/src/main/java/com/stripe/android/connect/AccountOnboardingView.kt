package com.stripe.android.connect

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.RestrictTo
import com.stripe.android.connect.webview.StripeConnectWebViewContainer
import com.stripe.android.connect.webview.StripeConnectWebViewContainerImpl

@PrivateBetaConnectSDK
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AccountOnboardingView private constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
    webViewContainerBehavior: StripeConnectWebViewContainerImpl,
) : FrameLayout(context, attrs, defStyleAttr),
    StripeConnectWebViewContainer by webViewContainerBehavior {

    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        embeddedComponentManager: EmbeddedComponentManager? = null,
    ) : this(
        context,
        attrs,
        defStyleAttr,
        StripeConnectWebViewContainerImpl(
            embeddedComponent = StripeEmbeddedComponent.ACCOUNT_ONBOARDING,
            embeddedComponentManager = embeddedComponentManager
        )
    )

    init {
        webViewContainerBehavior.initializeView(this)
    }
}
