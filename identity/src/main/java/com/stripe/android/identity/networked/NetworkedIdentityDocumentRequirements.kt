package com.stripe.android.identity.networked

import com.stripe.android.identity.networking.models.VerificationPage

internal data class NetworkedIdentityDocumentRequirements(
    val allowedDocumentTypes: Set<NetworkedIdentityDocumentType>,
    val requiresLiveCapture: Boolean
) {
    fun filter(
        documents: List<NetworkedIdentityDocument>,
        currentTimeSeconds: Long
    ): List<NetworkedIdentityDocument> = documents.filter { document ->
        document.documentType != NetworkedIdentityDocumentType.UNKNOWN &&
            document.documentType in allowedDocumentTypes &&
            (!requiresLiveCapture || document.liveCaptured == true) &&
            (document.expirationDate == null || document.expirationDate > currentTimeSeconds)
    }

    companion object {
        fun fromVerificationPage(page: VerificationPage): NetworkedIdentityDocumentRequirements =
            NetworkedIdentityDocumentRequirements(
                allowedDocumentTypes = page.documentSelect.idDocumentTypeAllowlist.keys.mapNotNull { type ->
                    when (type) {
                        "passport" -> NetworkedIdentityDocumentType.PASSPORT
                        "driving_license" -> NetworkedIdentityDocumentType.DRIVING_LICENSE
                        "id_card" -> NetworkedIdentityDocumentType.ID_CARD
                        else -> null
                    }
                }.toSet(),
                requiresLiveCapture = page.documentCapture.requireLiveCapture
            )
    }
}
