package com.stripe.android.connect.webview.serialization

import com.stripe.android.connect.AccountOnboardingProps
import com.stripe.android.connect.PrivateBetaConnectSDK
import kotlinx.serialization.Serializable

@Serializable
internal data class AccountOnboardingPropsJs(
    val setFullTermsOfServiceUrl: String?,
    val setRecipientTermsOfServiceUrl: String?,
    val setPrivacyPolicyUrl: String?,
    val setSkipTermsOfServiceCollection: Boolean?,
    val setCollectionOptions: CollectionOptionsJs?,
) {
    @Serializable
    data class CollectionOptionsJs(
        val fields: String?,
        val futureRequirements: String?,
    )
}

@OptIn(PrivateBetaConnectSDK::class)
internal fun AccountOnboardingProps.toJs(): AccountOnboardingPropsJs {
    return AccountOnboardingPropsJs(
        setFullTermsOfServiceUrl = fullTermsOfServiceUrl,
        setPrivacyPolicyUrl = privacyPolicyUrl,
        setRecipientTermsOfServiceUrl = recipientTermsOfServiceUrl,
        setSkipTermsOfServiceCollection = skipTermsOfServiceCollection,
        setCollectionOptions = collectionOptions?.let {
            AccountOnboardingPropsJs.CollectionOptionsJs(
                fields = it.fields?.value,
                futureRequirements = it.futureRequirements?.value,
            )
        }
    )
}
