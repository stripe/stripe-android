package com.stripe.android.crypto.onramp.model

import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * User attestation text that should be shown before recording acceptance.
 */
@Poko
internal class UserAttestation(
    val text: String,
    val version: String
)

@Serializable
internal data class UserAttestationResponse(
    @SerialName("text")
    val text: String,
    @SerialName("version")
    val version: String,
) {
    fun toUserAttestation(): UserAttestation {
        return UserAttestation(
            text = text,
            version = version
        )
    }
}
