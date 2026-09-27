package com.stripe.android.financialconnections.example.data.model

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class AccountHolder(
    @SerialName("type") val type: String,
    @SerialName("customer") val customer: String? = null,
    @SerialName("account") val account: String? = null,
)

@Keep
@Serializable
internal data class CreateAccountHolderBody(
    @SerialName("type") val type: String,
    @SerialName("flow") val flow: String?,
    @SerialName("custom_pk") val publishableKey: String?,
    @SerialName("custom_sk") val secretKey: String?,
    @SerialName("customer_email") val customerEmail: String?,
    @SerialName("test_environment") val testEnvironment: String?,
    @SerialName("test_mode") val testMode: Boolean?,
    @SerialName("stripe_account_id") val stripeAccountId: String?,
)

@Keep
@Serializable
internal data class CreateAccountHolderResponse(
    @SerialName("account_holder") val accountHolder: AccountHolder,
)

@Keep
@Serializable
internal data class CreateConsentBody(
    @SerialName("account_holder") val accountHolder: AccountHolder,
    @SerialName("locale") val locale: String?,
    @SerialName("flow") val flow: String?,
    @SerialName("custom_pk") val publishableKey: String?,
    @SerialName("custom_sk") val secretKey: String?,
    @SerialName("test_environment") val testEnvironment: String?,
    @SerialName("test_mode") val testMode: Boolean?,
    @SerialName("stripe_account_id") val stripeAccountId: String?,
)

@Keep
@Serializable
internal data class IssuedConsent(
    @SerialName("id") val id: String,
    @SerialName("consent_text") val consentText: String,
    @SerialName("locale") val locale: String,
    @SerialName("expires_at") val expiresAt: Long,
)

@Keep
@Serializable
internal data class CreateSetupIntentResponse(
    @SerialName("secret") val intentSecret: String,
    @SerialName("publishable_key") val publishableKey: String,
)

internal fun LinkAccountSessionBody.toCreateAccountHolderBody(type: String): CreateAccountHolderBody {
    return CreateAccountHolderBody(
        type = type,
        flow = flow,
        publishableKey = publishableKey,
        secretKey = secretKey,
        customerEmail = customerEmail,
        testEnvironment = testEnvironment,
        testMode = testMode,
        stripeAccountId = stripeAccountId,
    )
}

internal fun LinkAccountSessionBody.toCreateConsentBody(
    accountHolder: AccountHolder,
    locale: String?,
): CreateConsentBody {
    return CreateConsentBody(
        accountHolder = accountHolder,
        locale = locale,
        flow = flow,
        publishableKey = publishableKey,
        secretKey = secretKey,
        testEnvironment = testEnvironment,
        testMode = testMode,
        stripeAccountId = stripeAccountId,
    )
}
