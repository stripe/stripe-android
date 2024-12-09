package com.stripe.android.connect.webview.serialization

import com.stripe.android.connect.AccountOnboardingProps
import com.stripe.android.connect.PrivateBetaConnectSDK
import kotlinx.serialization.Serializable

@Serializable
internal data class AccountOnboardingPropsJs(
    val setFullTermsOfServiceUrl: String?,
    val setRecipientTermsOfServiceUrl: String?,
    val setPrivacyPolicyUrl: String?,
) : ComponentProps

@PrivateBetaConnectSDK
internal fun AccountOnboardingProps.toJs(): AccountOnboardingPropsJs {
    return AccountOnboardingPropsJs(
        setFullTermsOfServiceUrl = fullTermsOfServiceUrl,
        setPrivacyPolicyUrl = privacyPolicyUrl,
        setRecipientTermsOfServiceUrl = recipientTermsOfServiceUrl,
    )
}
