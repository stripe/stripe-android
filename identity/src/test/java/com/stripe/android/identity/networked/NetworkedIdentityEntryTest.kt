package com.stripe.android.identity.networked

import com.google.common.truth.Truth.assertThat
import com.stripe.android.identity.networking.models.NetworkedIdentityRoute
import org.junit.Test

internal class NetworkedIdentityEntryTest {

    @Test
    fun `a found saved document shows the chip with the provided email without calling Link`() =
        runNetworkedIdentityScenario(
            config = niConfig(merchantEmail = "person@example.com", savedDocumentAvailable = true)
        ) {
            assertThat(coordinator.entry.value).isEqualTo(
                NetworkedIdentityEntry(offersReuse = true, offersSave = true, savedIdEmail = "person@example.com")
            )
            // Offering never configures Link, opens the sheet or sends a code.
            assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.Idle)
        }

    @Test
    fun `a handed-in session's email is used for the chip`() = runNetworkedIdentityScenario(
        config = niConfig(merchantEmail = "merchant@example.com", savedDocumentAvailable = true),
        handoff = niHandoff(),
    ) {
        assertThat(coordinator.entry.value.savedIdEmail).isEqualTo("person@example.com")
    }

    @Test
    fun `reuse can be tried without a found document, without the chip`() = runNetworkedIdentityScenario(
        config = niConfig(merchantEmail = "person@example.com", savedDocumentAvailable = false)
    ) {
        assertThat(coordinator.entry.value).isEqualTo(
            NetworkedIdentityEntry(offersReuse = true, offersSave = true, savedIdEmail = null)
        )
    }

    @Test
    fun `without an email Link is offered without the chip`() = runNetworkedIdentityScenario(
        config = niConfig(savedDocumentAvailable = true)
    ) {
        assertThat(coordinator.entry.value.offersReuse).isTrue()
        assertThat(coordinator.entry.value.savedIdEmail).isNull()
    }

    @Test
    fun `without a merchant publishable key nothing is offered`() = runNetworkedIdentityScenario(
        config = niConfig(
            merchantPublishableKey = null,
            merchantEmail = "person@example.com",
            savedDocumentAvailable = true,
        )
    ) {
        assertThat(coordinator.entry.value).isEqualTo(
            NetworkedIdentityEntry(offersReuse = false, offersSave = false, savedIdEmail = null)
        )
    }

    @Test
    fun `sharing a document stops offering save`() = runNetworkedIdentityScenario {
        assertThat(coordinator.entry.value.offersSave).isTrue()
        reachDocuments()
        coordinator.shareSelectedDocument()
        runCurrent()
        repository.tokenCalls.awaitItem().response.complete(
            Result.success(NetworkedIdentityAssociationToken("token_1"))
        )
        runCurrent()
        actions.attachCalls.awaitItem().response.complete(Result.success(niActionPageData()))
        assertThat(verificationUpdates.awaitItem()).isEqualTo(niActionPageData())
        runCurrent()

        coordinator.continueAfterSuccess()

        assertThat(outcomes.awaitItem())
            .isEqualTo(NetworkedIdentityOutcome.DocumentShared)
        assertThat(coordinator.entry.value.offersSave).isFalse()
        assertThat(coordinator.entry.value.offersReuse).isFalse()
    }

    @Test
    fun `save route offers save but not reuse`() = runNetworkedIdentityScenario(
        config = niConfig(route = NetworkedIdentityRoute.Save, savedDocumentAvailable = true)
    ) {
        assertThat(coordinator.entry.value.offersReuse).isFalse()
        assertThat(coordinator.entry.value.offersSave).isTrue()
        assertThat(coordinator.entry.value.savedIdEmail).isNull()
    }

    @Test
    fun `a prepared save isn't offered again`() = runNetworkedIdentityScenario(
        config = niConfig(route = NetworkedIdentityRoute.ResumeSave)
    ) {
        assertThat(coordinator.entry.value.offersSave).isFalse()
    }

    @Test
    fun `reuse route without save enabled doesn't offer save`() = runNetworkedIdentityScenario(
        config = niConfig(saveEnabled = false)
    ) {
        assertThat(coordinator.entry.value.offersSave).isFalse()
    }

    @Test
    fun `skipped and resumed sessions don't offer new reuse`() {
        listOf(NetworkedIdentityRoute.OrdinaryIdentity, NetworkedIdentityRoute.ResumeReuse).forEach { route ->
            val config = niConfig(route = route, savedDocumentAvailable = true)
            val entry = NetworkedIdentityEntry(config, niHandoff(), route)

            assertThat(entry.offersReuse).isFalse()
            assertThat(entry.offersSave).isFalse()
            assertThat(entry.savedIdEmail).isNull()
        }
    }
}
