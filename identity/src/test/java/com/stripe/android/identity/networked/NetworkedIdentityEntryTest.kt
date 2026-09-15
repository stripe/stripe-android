package com.stripe.android.identity.networked

import com.google.common.truth.Truth.assertThat
import com.stripe.android.identity.networking.models.NetworkedIdentityRoute
import org.junit.Test

internal class NetworkedIdentityEntryTest {

    @Test
    fun `handed-in session offers reuse with its email and needs no lookup`() = runNetworkedIdentityScenario(
        handoff = niHandoff()
    ) {
        coordinator.lookUpProvidedAccountEmail()
        runCurrent()

        assertThat(coordinator.entry.value).isEqualTo(
            NetworkedIdentityEntry(
                reuseAvailable = true,
                offersSave = true,
                accountEmail = "person@example.com",
                needsEmail = false,
            )
        )
        assertThat(coordinator.entry.value.offersReuse).isTrue()
    }

    @Test
    fun `without an email Link is offered without an account and the sheet asks for one`() =
        runNetworkedIdentityScenario {
            coordinator.lookUpProvidedAccountEmail()
            runCurrent()

            assertThat(coordinator.entry.value).isEqualTo(
                NetworkedIdentityEntry(reuseAvailable = true, offersSave = true, accountEmail = null, needsEmail = true)
            )
            assertThat(coordinator.entry.value.offersReuse).isTrue()
        }

    @Test
    fun `provided email with a Link account offers reuse after the lookup`() = runNetworkedIdentityScenario(
        config = niConfig(merchantEmail = "person@example.com")
    ) {
        assertThat(coordinator.entry.value.offersReuse).isFalse()

        coordinator.lookUpProvidedAccountEmail()
        runCurrent()
        linkSession.configureCalls.awaitItem().response.complete(Result.success(Unit))
        runCurrent()
        val lookup = linkSession.lookupCalls.awaitItem()
        assertThat(lookup.email).isEqualTo("person@example.com")
        lookup.response.complete(Result.success(niAccount()))
        runCurrent()

        assertThat(coordinator.entry.value.accountEmail).isEqualTo("person@example.com")
        assertThat(coordinator.entry.value.offersReuse).isTrue()
        // Checking never opens the sheet or sends a code.
        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.Idle)
    }

    @Test
    fun `provided email without a Link account hides reuse`() = runNetworkedIdentityScenario(
        config = niConfig(merchantEmail = "person@example.com")
    ) {
        coordinator.lookUpProvidedAccountEmail()
        runCurrent()
        linkSession.configureCalls.awaitItem().response.complete(Result.success(Unit))
        runCurrent()
        linkSession.lookupCalls.awaitItem().response.complete(Result.success(null))
        runCurrent()

        assertThat(coordinator.entry.value.accountEmail).isNull()
        assertThat(coordinator.entry.value.offersReuse).isFalse()
        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.Idle)
    }

    @Test
    fun `without a merchant publishable key nothing is offered`() = runNetworkedIdentityScenario(
        config = niConfig(merchantPublishableKey = null, merchantEmail = "person@example.com")
    ) {
        coordinator.lookUpProvidedAccountEmail()
        runCurrent()

        assertThat(coordinator.entry.value.linkAvailable).isFalse()
        assertThat(coordinator.entry.value.offersReuse).isFalse()
    }

    @Test
    fun `Link is configured once for the check and the attempt`() = runNetworkedIdentityScenario(
        config = niConfig(merchantEmail = "person@example.com")
    ) {
        coordinator.lookUpProvidedAccountEmail()
        runCurrent()
        linkSession.configureCalls.awaitItem().response.complete(Result.success(Unit))
        runCurrent()
        linkSession.lookupCalls.awaitItem().response.complete(Result.success(niAccount()))
        runCurrent()

        coordinator.startReuse()
        runCurrent()

        // No second configure call or lookup: the attempt continues with the account the check found.
        assertThat(linkSession.startVerificationCalls.awaitItem().isResend).isFalse()
        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.OtpStartPending)
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
        runCurrent()

        coordinator.continueAfterSuccess()

        assertThat(outcomes.awaitItem())
            .isEqualTo(NetworkedIdentityOutcome.DocumentShared(niDocument(), niActionPageData()))
        assertThat(coordinator.entry.value.offersSave).isFalse()
        assertThat(coordinator.entry.value.reuseAvailable).isFalse()
    }

    @Test
    fun `save route offers save but not reuse`() = runNetworkedIdentityScenario(
        config = niConfig(route = NetworkedIdentityRoute.Save)
    ) {
        assertThat(coordinator.entry.value.reuseAvailable).isFalse()
        assertThat(coordinator.entry.value.offersSave).isTrue()
    }

    @Test
    fun `resumed save still offers save without the save flag`() = runNetworkedIdentityScenario(
        config = niConfig(route = NetworkedIdentityRoute.ResumeSave, saveAvailable = false)
    ) {
        assertThat(coordinator.entry.value.offersSave).isTrue()
        assertThat(coordinator.hasPreparedSave.value).isTrue()
    }

    @Test
    fun `reuse route without the save flag doesn't offer save`() = runNetworkedIdentityScenario(
        config = niConfig(saveAvailable = false)
    ) {
        assertThat(coordinator.entry.value.offersSave).isFalse()
    }

    @Test
    fun `skipped and resumed sessions don't offer new reuse`() {
        listOf(NetworkedIdentityRoute.OrdinaryIdentity, NetworkedIdentityRoute.ResumeReuse).forEach { route ->
            val entry = NetworkedIdentityEntry(niConfig(route = route), niHandoff(), route, accountEmail = null)

            assertThat(entry.reuseAvailable).isFalse()
            assertThat(entry.offersReuse).isFalse()
            assertThat(entry.offersSave).isFalse()
        }
    }
}
