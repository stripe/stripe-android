package com.stripe.android.identity.networked

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SeededDocumentsNetworkedIdentityRepositoryTest {

    @Test
    fun `listDocuments returns live-captured, unexpired seeded documents`() =
        runScenario {
            val documents = repository.listDocuments(CREDENTIALS).getOrThrow()

            assertThat(documents.map { it.documentType }).containsExactly(
                NetworkedIdentityDocumentType.PASSPORT,
                NetworkedIdentityDocumentType.DRIVING_LICENSE,
            )
            assertThat(documents.all { it.liveCaptured == true }).isTrue()
            assertThat(documents.all { (it.expirationDate ?: 0) > NOW }).isTrue()
        }

    @Test
    fun `seeded documents pass the document requirements filter`() = runScenario {
        val requirements = NetworkedIdentityDocumentRequirements(
            allowedDocumentTypes = setOf(NetworkedIdentityDocumentType.PASSPORT),
            requiresLiveCapture = true,
        )

        val eligible = requirements.filter(repository.listDocuments(CREDENTIALS).getOrThrow(), NOW)

        assertThat(eligible.map { it.documentType }).containsExactly(NetworkedIdentityDocumentType.PASSPORT)
    }

    private fun runScenario(block: suspend Scenario.() -> Unit) = runTest {
        Scenario(repository = SeededDocumentsNetworkedIdentityRepository(currentTimeSeconds = { NOW })).block()
    }

    private data class Scenario(val repository: SeededDocumentsNetworkedIdentityRepository)

    private companion object {
        const val NOW = 1_700_000_000L
        val CREDENTIALS = NetworkedIdentityCredentials(linkSessionKey = "lsk_session")
    }
}
