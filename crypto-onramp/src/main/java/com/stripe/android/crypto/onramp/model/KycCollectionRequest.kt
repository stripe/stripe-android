package com.stripe.android.crypto.onramp.model

import androidx.annotation.RestrictTo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Internal API request model for KYC data collection.
 * This represents the exact structure expected by the Stripe API.
 */
@Serializable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal data class KycCollectionRequest(
    @SerialName("first_name")
    val firstName: String,
    @SerialName("last_name")
    val lastName: String,
    @SerialName("id_number")
    val idNumber: String?,
    @SerialName("id_type")
    val idType: String?,
    @SerialName("dob")
    val dateOfBirth: DateOfBirth,
    @SerialName("nationalities")
    val nationalities: List<String>?,
    @SerialName("birth_country")
    val birthCountry: String?,
    @SerialName("birth_city")
    val birthCity: String?,
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
         * Converts SDK KycInfo model to internal API request model.
         */
        fun fromKycInfo(
            kycInfo: KycInfo,
            credentials: CryptoCustomerRequestParams.Credentials
        ): KycCollectionRequest {
            return KycCollectionRequest(
                firstName = kycInfo.firstName,
                lastName = kycInfo.lastName,
                idNumber = kycInfo.idNumber,
                idType = "social_security_number",
                dateOfBirth = kycInfo.dateOfBirth,
                nationalities = null,
                birthCountry = null,
                birthCity = null,
                city = kycInfo.address.city,
                country = kycInfo.address.country,
                line1 = kycInfo.address.line1,
                line2 = kycInfo.address.line2,
                postalCode = kycInfo.address.postalCode,
                state = kycInfo.address.state,
                credentials = credentials
            )
        }
    }
}
