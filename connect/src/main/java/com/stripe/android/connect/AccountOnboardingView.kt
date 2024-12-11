package com.stripe.android.connect

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.RestrictTo
import androidx.core.content.withStyledAttributes
import com.stripe.android.connect.webview.StripeConnectWebViewContainer
import com.stripe.android.connect.webview.StripeConnectWebViewContainerImpl
import com.stripe.android.connect.webview.serialization.SetOnExit
import com.stripe.android.connect.webview.serialization.SetterFunctionCalledMessage
import com.stripe.android.connect.webview.serialization.toJs
import com.stripe.android.connect.webview.serialization.toJsonObject
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

@PrivateBetaConnectSDK
class AccountOnboardingView private constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
    private val webViewContainerBehavior: StripeConnectWebViewContainerImpl<AccountOnboardingListener>,
) : FrameLayout(context, attrs, defStyleAttr),
    StripeConnectWebViewContainer<AccountOnboardingListener> by webViewContainerBehavior {

    @JvmOverloads
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        embeddedComponentManager: EmbeddedComponentManager? = null,
        props: AccountOnboardingProps? = null,
        listener: AccountOnboardingListener? = null,
    ) : this(
        context,
        attrs,
        defStyleAttr,
        StripeConnectWebViewContainerImpl(
            embeddedComponent = StripeEmbeddedComponent.ACCOUNT_ONBOARDING,
            embeddedComponentManager = embeddedComponentManager,
            props = props?.toJs()?.toJsonObject(),
            listener = listener,
            listenerDelegate = AccountOnboardingListenerDelegate,
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
            webViewContainerBehavior.updateProps(
                props = props.toJs().toJsonObject(),
                merge = true
            )
        }

        webViewContainerBehavior.initializeView(this)
    }

    /**
     * Updates the component props to [props]. If [merge] is true, existing values will not be overwritten by
     * null values.
     */
    fun updateProps(props: AccountOnboardingProps, merge: Boolean) {
        webViewContainerBehavior.updateProps(
            props = props.toJs().toJsonObject(),
            merge = merge
        )
    }
}

@PrivateBetaConnectSDK
@Parcelize
@Poko
class AccountOnboardingProps(
    val fullTermsOfServiceUrl: String? = null,
    val recipientTermsOfServiceUrl: String? = null,
    val privacyPolicyUrl: String? = null,
) : Parcelable

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
