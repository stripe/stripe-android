package com.stripe.android.identity.networked

// These models deliberately are not Parcelable or serializable: credentials belong to one flow in memory.
internal data class NetworkedIdentityCredentials(
    val publishableKey: String,
    val sessionClientSecret: String,
) {
    override fun toString(): String = "NetworkedIdentityCredentials([redacted])"
}

internal sealed interface NetworkedIdentityLookup {
    data class Found(
        val session: NetworkedIdentityConsumerSession,
        val publishableKey: String,
        val accountId: String?,
        val authSessionClientSecret: String?,
        val emailOtpRequiresAdditionalInfo: Boolean?,
        val emailOtpVerifyPhoneDespiteSmsOtp: Boolean?,
        val experiments: List<NetworkedIdentityExperiment>,
    ) : NetworkedIdentityLookup {
        override fun toString(): String = "NetworkedIdentityLookup.Found([redacted])"
    }

    data class NotFound(val errorMessage: String?) : NetworkedIdentityLookup {
        override fun toString(): String = "NetworkedIdentityLookup.NotFound([redacted])"
    }
}

internal data class NetworkedIdentityExperiment(
    val experimentName: String,
    val variant: String,
    val responseId: String?,
)

internal data class NetworkedIdentityConsumerSession(
    val clientSecret: String,
    val emailAddress: String,
    val redactedPhoneNumber: String,
    val redactedFormattedPhoneNumber: String,
    val unredactedPhoneNumber: String?,
    val phoneNumberCountry: String?,
    val verificationSessions: List<NetworkedIdentityVerificationSession>,
) {
    override fun toString(): String = "NetworkedIdentityConsumerSession([redacted])"
}

internal data class NetworkedIdentityVerificationSession(
    val id: String?,
    val type: NetworkedIdentityVerificationType,
    val state: NetworkedIdentityVerificationState,
    val verificationToken: String?,
) {
    override fun toString(): String = "NetworkedIdentityVerificationSession(type=$type, state=$state)"
}

internal enum class NetworkedIdentityVerificationType {
    SMS, EMAIL, WEBAUTHN, ADMIN, SIGNUP, SUPPORT_TIER_1, VERIFICATION_TYPE_INVALID, UNKNOWN,
}

internal enum class NetworkedIdentityVerificationState {
    STARTED, FAILED, VERIFIED, CANCELED, EXPIRED, VERIFICATION_STATE_INVALID, UNKNOWN,
}

internal data class NetworkedIdentitySessionResponse(
    val session: NetworkedIdentityConsumerSession,
    val authSessionClientSecret: String?,
) {
    override fun toString(): String = "NetworkedIdentitySessionResponse([redacted])"
}

internal data class NetworkedIdentitySignUpResponse(
    val session: NetworkedIdentityConsumerSession,
    val publishableKey: String,
    val accountId: String,
    val authSessionClientSecret: String?,
) {
    override fun toString(): String = "NetworkedIdentitySignUpResponse([redacted])"
}

internal data class NetworkedIdentityDocument(
    val id: String,
    val documentType: NetworkedIdentityDocumentType,
    val created: Long,
    val country: String?,
    val region: String?,
    val redactedDocumentNumber: String?,
    val expirationDate: Long?,
    val liveCaptured: Boolean?,
) {
    override fun toString(): String = "NetworkedIdentityDocument(type=$documentType)"
}

internal enum class NetworkedIdentityDocumentType(val value: String) {
    PASSPORT("passport"),
    DRIVING_LICENSE("driving_license"),
    ID_CARD("id_card"),
    UNKNOWN("unknown"),
}

internal data class NetworkedIdentityAssociationToken(val associationToken: String) {
    override fun toString(): String = "NetworkedIdentityAssociationToken([redacted])"
}

internal data class NetworkedIdentityExtendSessionResponse(val consumerSessionClientSecret: String?) {
    override fun toString(): String = "NetworkedIdentityExtendSessionResponse([redacted])"
}

internal enum class NetworkedIdentityCountryInferringMethod {
    PHONE_NUMBER, DEFAULT_US, UNKNOWN,
}

internal data class NetworkedIdentitySignUpRequest(
    val emailAddress: String,
    val phoneNumber: String,
    val country: String,
    val countryInferringMethod: NetworkedIdentityCountryInferringMethod,
    val locale: String,
    val defaultOptInEnabled: Boolean?,
    val changedPhoneNumber: Boolean?,
    val checkedOptInBox: Boolean?,
    val legalName: String?,
    val hcaptchaResponse: String?,
    val hcaptchaKey: String?,
    val sessionId: String?,
    val authSessionSecrets: List<String>,
) {
    override fun toString(): String = "NetworkedIdentitySignUpRequest([redacted])"
}
