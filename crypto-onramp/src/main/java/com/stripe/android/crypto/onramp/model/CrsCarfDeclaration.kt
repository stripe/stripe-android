package com.stripe.android.crypto.onramp.model

import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * CRS/CARF declaration text that should be shown before recording acceptance.
 */
@Poko
internal class CrsCarfDeclaration(
    val text: String,
    val version: String
)

@Serializable
internal data class CrsCarfDeclarationResponse(
    @SerialName("text")
    val text: String,
    @SerialName("version")
    val version: String,
) {
    fun toDeclaration(): CrsCarfDeclaration {
        return CrsCarfDeclaration(
            text = text,
            version = version
        )
    }
}
