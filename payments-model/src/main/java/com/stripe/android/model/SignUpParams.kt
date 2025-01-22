package com.stripe.android.model

import androidx.annotation.RestrictTo
import java.util.Locale

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
data class SignUpParams(
    val email: String,
    val phoneNumber: String,
    val country: String,
    val name: String?,
    val locale: Locale?,
    val amount: Long?,
    val currency: String?,
    val incentiveEligibilitySession: IncentiveEligibilitySession?,
    val requestSurface: String,
    val consentAction: ConsumerSignUpConsentAction,
    val verificationToken: String? = null,
    val appId: String? = null
) {
    fun toParamMap(): Map<String, Any> {
        val params = mutableMapOf<String, Any>(
            "email_address" to email.lowercase(),
            "phone_number" to phoneNumber,
            "country" to country,
            "country_inferring_method" to "PHONE_NUMBER",
            "consent_action" to consentAction.value,
            "request_surface" to requestSurface,
        )

        if (amount != null) {
            params["amount"] = amount
        }

        if (currency != null) {
            params["currency"] = currency
        }

        locale?.let {
            params["locale"] = it.toLanguageTag()
        }

        name?.let {
            params["legal_name"] = it
        }

        verificationToken?.let {
            params["android_verification_token"] = it
        }

        appId?.let {
            params["app_id"] = it
        }

        val incentiveParams = incentiveEligibilitySession?.toParamMap().orEmpty()
        params.putAll(incentiveParams)

        return params
    }
}
