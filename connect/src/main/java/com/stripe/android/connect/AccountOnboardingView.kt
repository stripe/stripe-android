package com.stripe.android.connect

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.RestrictTo
import com.stripe.android.connect.webview.StripeConnectWebViewContainer
import com.stripe.android.connect.webview.StripeConnectWebViewContainerImpl
import com.stripe.android.connect.webview.serialization.SetOnExit
import com.stripe.android.connect.webview.serialization.SetterFunctionCalledMessage

@PrivateBetaConnectSDK
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AccountOnboardingView private constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
    webViewContainerBehavior: StripeConnectWebViewContainerImpl<AccountOnboardingListener>,
) : FrameLayout(context, attrs, defStyleAttr),
    StripeConnectWebViewContainer<AccountOnboardingListener> by webViewContainerBehavior {

    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        embeddedComponentManager: EmbeddedComponentManager? = null,
        listener: AccountOnboardingListener? = null,
    ) : this(
        context,
        attrs,
        defStyleAttr,
        StripeConnectWebViewContainerImpl(
            embeddedComponent = StripeEmbeddedComponent.ACCOUNT_ONBOARDING,
            embeddedComponentManager = embeddedComponentManager,
            listener = listener,
            listenerDelegate = AccountOnboardingListenerDelegate
        )
    )

    init {
        webViewContainerBehavior.initializeView(this)
    }
}

@PrivateBetaConnectSDK
interface AccountOnboardingListener : StripeEmbeddedComponentListener {
    /**
     * The connected account has exited the onboarding process.
     */
    fun onExit() {}
}

@OptIn(PrivateBetaConnectSDK::class)
internal object AccountOnboardingListenerDelegate : ComponentListenerDelegate<AccountOnboardingListener> {
    override fun AccountOnboardingListener.delegate(message: SetterFunctionCalledMessage) {
        when (message.value) {
            is SetOnExit -> onExit()
            else -> {
                // Ignore.
            }
        }
    }
}
