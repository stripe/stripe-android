package com.stripe.android.connect.webview.serialization

import com.stripe.android.connect.AccountOnboardingProps
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
        val requirements: RequirementsJs?,
    )

    @Serializable
    data class RequirementsJs(
        val only: List<String>?,
        val exclude: List<String>?,
    )
}

internal fun AccountOnboardingProps.toJs(): AccountOnboardingPropsJs {
    return AccountOnboardingPropsJs(
        setFullTermsOfServiceUrl = fullTermsOfServiceUrl,
        setPrivacyPolicyUrl = privacyPolicyUrl,
        setRecipientTermsOfServiceUrl = recipientTermsOfServiceUrl,
        setSkipTermsOfServiceCollection = skipTermsOfServiceCollection,
        setCollectionOptions = collectionOptions?.toJs()
    )
}

internal fun AccountOnboardingProps.CollectionOptions.toJs(): AccountOnboardingPropsJs.CollectionOptionsJs {
    return AccountOnboardingPropsJs.CollectionOptionsJs(
        fields = fields?.value,
        futureRequirements = futureRequirements?.value,
        requirements = requirements?.let { requirements ->
            when (requirements) {
                is AccountOnboardingProps.RequirementsOption.Only ->
                    AccountOnboardingPropsJs.RequirementsJs(only = requirements.only, exclude = null)
                is AccountOnboardingProps.RequirementsOption.Exclude ->
                    AccountOnboardingPropsJs.RequirementsJs(only = null, exclude = requirements.exclude)
            }
        }
    )
}
