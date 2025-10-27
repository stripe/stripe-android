package com.stripe.android.crypto.onramp.model

import androidx.annotation.RestrictTo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Internal API request model for KYC refreshing.
 * This represents the exact structure expected by the Stripe API.
 */
@Serializable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal data class KycRefreshRequest(
    @SerialName("first_name")
    val firstName: String,
    @SerialName("last_name")
    val lastName: String,
    @SerialName("id_number_last4")
    val idNumberLastFour: String?,
    @SerialName("id_type")
    val idType: String?,
    @SerialName("dob")
    val dateOfBirth: DateOfBirth,
    @SerialName("city")
    val city: String?,
    @SerialName("country")
    val country: String?,
    @SerialName("line1")
    val line1: String?,
    @SerialName("line2")
    val line2: String?,
    @SerialName("zip")
    val postalCode: String?,
    @SerialName("state")
    val state: String?,
    @SerialName("credentials")
    val credentials: CryptoCustomerRequestParams.Credentials
) {
    companion object Companion {
        /**
         * Converts SDK KycInfo model to an internal API refresh request model.
         */
        fun fromRefreshKycInfo(
            kycInfo: RefreshKycInfo,
            credentials: CryptoCustomerRequestParams.Credentials
        ): KycRefreshRequest {
            return KycRefreshRequest(
                firstName = kycInfo.firstName,
                lastName = kycInfo.lastName,
                idNumberLastFour = kycInfo.idNumberLastFour,
                idType = SOCIAL_SECURITY_NUMBER,
                dateOfBirth = kycInfo.dateOfBirth,
                city = kycInfo.address.city,
                country = kycInfo.address.country,
                line1 = kycInfo.address.line1,
                line2 = kycInfo.address.line2,
                postalCode = kycInfo.address.postalCode,
                state = kycInfo.address.state,
                credentials = credentials
            )
        }

        /**
         * Currently, we only support SSN for identity verification in the US.
         */
        private const val SOCIAL_SECURITY_NUMBER = "social_security_number"
    }
}
