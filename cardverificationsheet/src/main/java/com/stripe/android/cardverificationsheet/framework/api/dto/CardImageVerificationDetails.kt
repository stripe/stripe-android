package com.stripe.android.cardverificationsheet.framework.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class CardImageVerificationDetailsRequest(
    @SerialName("client_secret") val cardImageVerificationSecret: String
)

@Serializable
internal data class CardImageVerificationDetailsResult(
    @SerialName("expected_card") val expectedCard: CardImageVerificationDetailsResultCard?
)

@Serializable
internal data class CardImageVerificationDetailsResultCard(
    @SerialName("issuer") val issuer: String?,
    @SerialName("last4") val lastFour: String?,
)
