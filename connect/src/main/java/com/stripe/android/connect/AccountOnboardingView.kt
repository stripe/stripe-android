package com.stripe.android.connect

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
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
    cacheKey: String?,
    webViewContainerBehavior: StripeConnectWebViewContainerImpl<AccountOnboardingListener, AccountOnboardingProps>,
) : StripeComponentView<AccountOnboardingListener, AccountOnboardingProps>(context, attrs, defStyleAttr),
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
        cacheKey: String? = null,
    ) : this(
        context = context,
        attrs = attrs,
        defStyleAttr = defStyleAttr,
        cacheKey = cacheKey,
        webViewContainerBehavior = StripeConnectWebViewContainerImpl(
            context = context,
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
        var xmlCacheKey: String? = null
        context.withStyledAttributes(attrs, R.styleable.StripeConnectWebViewContainer, defStyleAttr, 0) {
            xmlCacheKey = getString(R.styleable.StripeConnectWebViewContainer_stripeWebViewCacheKey)
        }
        webViewContainerBehavior.initializeView(this, cacheKey ?: xmlCacheKey)
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

    /**
     * If true, embedded onboarding skips terms of service collection and you must
     * [collect terms acceptance yourself](https://docs.stripe.com/connect/updating-service-agreements#indicating-acceptance).
     */
    @Suppress("MaxLineLength")
    val skipTermsOfServiceCollection: Boolean? = null,

    /**
     * Customizes collecting `currently_due` or `eventually_due` requirements and controls whether to include
     * [future requirements](https://docs.stripe.com/api/accounts/object#account_object-future_requirements).
     * Specifying `eventually_due` collects both `eventually_due` and `currently_due` requirements.
     */
    val collectionOptions: CollectionOptions? = null,
) : ComponentProps {

    @PrivateBetaConnectSDK
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Parcelize
    @Poko
    class CollectionOptions(
        val fields: FieldOption? = null,
        val futureRequirements: FutureRequirementOption? = null,
    ) : Parcelable

    @PrivateBetaConnectSDK
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    enum class FieldOption(internal val value: String) {
        CURRENTLY_DUE("currently_due"),
        EVENTUALLY_DUE("eventually_due"),
    }

    @PrivateBetaConnectSDK
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    enum class FutureRequirementOption(internal val value: String) {
        OMIT("omit"),
        INCLUDE("include"),
    }
}

@PrivateBetaConnectSDK
@RestrictTo(RestrictTo.Scope.LIBRARY)
interface AccountOnboardingListener : StripeEmbeddedComponentListener {
    /**
     * The connected account has exited the onboarding process.
     */
    fun onExit() {}
}

@OptIn(PrivateBetaConnectSDK::class)
internal object AccountOnboardingListenerDelegate : ComponentListenerDelegate<AccountOnboardingListener>() {
    override fun delegate(listener: AccountOnboardingListener, message: SetterFunctionCalledMessage) {
        when (message.value) {
            is SetOnExit -> listener.onExit()
            else -> {
                // Ignore.
            }
        }
    }
}
