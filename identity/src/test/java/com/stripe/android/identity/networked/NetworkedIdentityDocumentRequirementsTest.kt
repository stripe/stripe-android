package com.stripe.android.identity.networked

import com.google.common.truth.Truth.assertThat
import com.stripe.android.identity.SUCCESS_VERIFICATION_PAGE_NOT_REQUIRE_LIVE_CAPTURE
import org.junit.Test

internal class NetworkedIdentityDocumentRequirementsTest {
    @Test
    fun `unknown document types cannot qualify even when in allowlist`() {
        val requirements = NetworkedIdentityDocumentRequirements(NetworkedIdentityDocumentType.entries.toSet(), false)

        assertThat(requirements.filter(listOf(document(type = NetworkedIdentityDocumentType.UNKNOWN)), NOW)).isEmpty()
    }

    @Test
    fun `known types outside current allowlist are rejected`() {
        assertThat(requirements(false).filter(listOf(document(type = NetworkedIdentityDocumentType.ID_CARD)), NOW))
            .isEmpty()
    }

    @Test
    fun `live capture must be explicitly true when required`() {
        val documents = listOf(
            document(id = "absent", liveCaptured = null),
            document(id = "false", liveCaptured = false),
            document(id = "true", liveCaptured = true)
        )

        assertThat(requirements(true).filter(documents, NOW).map { it.id }).containsExactly("true")
    }

    @Test
    fun `non-live documents qualify when live capture is not required`() {
        val documents = listOf(
            document(id = "absent", liveCaptured = null),
            document(id = "false", liveCaptured = false)
        )

        assertThat(requirements(false).filter(documents, NOW).map { it.id })
            .containsExactly("absent", "false").inOrder()
    }

    @Test
    fun `expiration equal to current second is expired`() {
        assertThat(requirements(false).filter(listOf(document(expirationDate = NOW)), NOW)).isEmpty()
    }

    @Test
    fun `past expiration is rejected`() {
        assertThat(requirements(false).filter(listOf(document(expirationDate = NOW - 1)), NOW)).isEmpty()
    }

    @Test
    fun `future expiration in seconds is accepted`() {
        assertThat(requirements(false).filter(listOf(document(expirationDate = NOW + 1)), NOW)).hasSize(1)
    }

    @Test
    fun `missing expiration is accepted`() {
        assertThat(requirements(false).filter(listOf(document(expirationDate = null)), NOW)).hasSize(1)
    }

    @Test
    fun `eligible documents retain backend order`() {
        val documents = listOf(
            document(id = "second"),
            document(id = "excluded", expirationDate = NOW),
            document(id = "first")
        )

        assertThat(requirements(false).filter(documents, NOW).map { it.id })
            .containsExactly("second", "first").inOrder()
    }

    @Test
    fun `requirements use current page allowlist and capture setting`() {
        val original = SUCCESS_VERIFICATION_PAGE_NOT_REQUIRE_LIVE_CAPTURE
        val page = original.copy(
            documentSelect = original.documentSelect.copy(
                idDocumentTypeAllowlist = mapOf("driving_license" to "Driver license", "future_type" to "Future ID")
            ),
            documentCapture = original.documentCapture.copy(requireLiveCapture = true)
        )
        val requirements = NetworkedIdentityDocumentRequirements.fromVerificationPage(page)

        assertThat(requirements.allowedDocumentTypes).containsExactly(NetworkedIdentityDocumentType.DRIVING_LICENSE)
        assertThat(requirements.requiresLiveCapture).isTrue()
    }

    private fun requirements(requiresLiveCapture: Boolean) = NetworkedIdentityDocumentRequirements(
        allowedDocumentTypes = setOf(NetworkedIdentityDocumentType.PASSPORT),
        requiresLiveCapture = requiresLiveCapture
    )

    private fun document(
        id: String = "document",
        type: NetworkedIdentityDocumentType = NetworkedIdentityDocumentType.PASSPORT,
        expirationDate: Long? = null,
        liveCaptured: Boolean? = null
    ) = NetworkedIdentityDocument(
        id = id,
        documentType = type,
        created = NOW - 100,
        country = null,
        region = null,
        redactedDocumentNumber = "••1234",
        expirationDate = expirationDate,
        liveCaptured = liveCaptured
    )

    private companion object {
        const val NOW = 1_789_084_800L
    }
}
