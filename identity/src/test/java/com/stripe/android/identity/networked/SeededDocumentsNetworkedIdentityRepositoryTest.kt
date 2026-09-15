package com.stripe.android.identity.networked

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SeededDocumentsNetworkedIdentityRepositoryTest {

    @Test
    fun `listDocuments returns live-captured, unexpired seeded documents without calling the backend`() =
        runScenario {
            val documents = repository.listDocuments(CREDENTIALS).getOrThrow()

            assertThat(documents.map { it.documentType }).containsExactly(
                NetworkedIdentityDocumentType.PASSPORT,
                NetworkedIdentityDocumentType.DRIVING_LICENSE,
            )
            assertThat(documents.all { it.liveCaptured == true }).isTrue()
            assertThat(documents.all { (it.expirationDate ?: 0) > NOW }).isTrue()
            assertThat(documents.all { it.id.startsWith(SeededDocumentsNetworkedIdentityRepository.SEEDED_ID_PREFIX) })
                .isTrue()
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

    @Test
    fun `createAssociationToken for a seeded document returns a token without calling the backend`() =
        runScenario {
            val token = repository.createAssociationToken(
                CREDENTIALS,
                "${SeededDocumentsNetworkedIdentityRepository.SEEDED_ID_PREFIX}passport",
            ).getOrThrow()

            assertThat(token.associationToken).startsWith("seeded_token_")
        }

    @Test
    fun `attaching a seeded token succeeds without calling the backend`() = runTest {
        val delegate = FakeNetworkedIdentityActions()
        val actions = SeededDocumentsNetworkedIdentityActions(delegate)

        val data = actions.attachDocument("seeded_token_seeded_iddoc_passport").getOrThrow()

        assertThat(data.id).isEqualTo(delegate.verificationSessionId)
        delegate.ensureAllEventsConsumed()
    }

    @Test
    fun `attaching a real token goes to the backend`() = runTest {
        val delegate = FakeNetworkedIdentityActions()
        val actions = SeededDocumentsNetworkedIdentityActions(delegate)

        val attach = async { actions.attachDocument("token_1") }
        val call = delegate.attachCalls.awaitItem()
        assertThat(call.associationToken).isEqualTo("token_1")
        call.response.complete(Result.success(niActionPageData()))

        assertThat(attach.await().isSuccess).isTrue()
        delegate.ensureAllEventsConsumed()
    }

    private fun runScenario(block: suspend Scenario.() -> Unit) = runTest {
        val delegate = FakeNetworkedIdentityRepository()
        Scenario(
            repository = SeededDocumentsNetworkedIdentityRepository(delegate = delegate, currentTimeSeconds = { NOW }),
        ).block()
        delegate.ensureAllEventsConsumed()
    }

    private data class Scenario(val repository: SeededDocumentsNetworkedIdentityRepository)

    private companion object {
        const val NOW = 1_700_000_000L
        val CREDENTIALS = NetworkedIdentityCredentials(publishableKey = "pk_consumer", sessionClientSecret = "secret")
    }
}
