package com.stripe.android.connect

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.RestrictTo
import androidx.core.content.withStyledAttributes
import com.stripe.android.connect.webview.StripeConnectWebViewContainer
import com.stripe.android.connect.webview.StripeConnectWebViewContainerImpl
import com.stripe.android.connect.webview.serialization.SetOnExit
import com.stripe.android.connect.webview.serialization.SetterFunctionCalledMessage
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

@PrivateBetaConnectSDK
class AccountOnboardingView private constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
    webViewContainerBehavior: StripeConnectWebViewContainerImpl<AccountOnboardingListener, AccountOnboardingProps>,
) : FrameLayout(context, attrs, defStyleAttr),
    StripeConnectWebViewContainer<AccountOnboardingListener, AccountOnboardingProps> by webViewContainerBehavior {

    @JvmOverloads
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        embeddedComponentManager: EmbeddedComponentManager? = null,
        listener: AccountOnboardingListener? = null,
        props: AccountOnboardingProps? = null,
    ) : this(
        context,
        attrs,
        defStyleAttr,
        StripeConnectWebViewContainerImpl(
            embeddedComponent = StripeEmbeddedComponent.ACCOUNT_ONBOARDING,
            embeddedComponentManager = embeddedComponentManager,
            listener = listener,
            listenerDelegate = AccountOnboardingListenerDelegate,
            props = props,
        )
    )

    init {
        context.withStyledAttributes(attrs, R.styleable.StripeAccountOnboardingView, defStyleAttr, 0) {
            val props = AccountOnboardingProps(
                fullTermsOfServiceUrl = getString(
                    R.styleable.StripeAccountOnboardingView_stripeFullTermsOfServiceUrl
                ),
                recipientTermsOfServiceUrl = getString(
                    R.styleable.StripeAccountOnboardingView_stripeRecipientTermsOfServiceUrl
                ),
                privacyPolicyUrl = getString(
                    R.styleable.StripeAccountOnboardingView_stripePrivacyPolicyUrl
                ),
            )
            webViewContainerBehavior.setPropsFromXml(props)
        }
        webViewContainerBehavior.initializeView(this)
    }
}

@PrivateBetaConnectSDK
@RestrictTo(RestrictTo.Scope.LIBRARY)
@Parcelize
@Poko
class AccountOnboardingProps(
    /**
     * URL to your [full terms of service agreement](https://docs.stripe.com/connect/service-agreement-types#full).
     */
    val fullTermsOfServiceUrl: String? = null,

    /**
     * URL to your
     * [recipient terms of service](https://docs.stripe.com/connect/service-agreement-types#recipient) agreement.
     */
    val recipientTermsOfServiceUrl: String? = null,

    /**
     * Absolute URL to your privacy policy.
     */
    val privacyPolicyUrl: String? = null,
) : ComponentProps

@PrivateBetaConnectSDK
@RestrictTo(RestrictTo.Scope.LIBRARY)
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
