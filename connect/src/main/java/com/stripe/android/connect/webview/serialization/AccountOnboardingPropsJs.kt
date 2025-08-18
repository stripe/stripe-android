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
        setCollectionOptions = collectionOptions?.let {
            AccountOnboardingPropsJs.CollectionOptionsJs(
                fields = it.fields?.value,
                futureRequirements = it.futureRequirements?.value,
                requirements = it.requirements?.let { req ->
                    when (req) {
                        is AccountOnboardingProps.RequirementsOption.Only ->
                            AccountOnboardingPropsJs.RequirementsJs(only = req.only, exclude = null)
                        is AccountOnboardingProps.RequirementsOption.Exclude ->
                            AccountOnboardingPropsJs.RequirementsJs(only = null, exclude = req.exclude)
                    }
                }
            )
        }
    )
}
