package com.stripe.android.identity.networked

import com.google.common.truth.Truth.assertThat
import org.junit.Test

internal class NetworkedIdentityEntryTest {

    @Test
    fun `handed-in session offers reuse with its email and needs no lookup`() = runNetworkedIdentityScenario(
        handoff = niHandoff()
    ) {
        coordinator.refreshEntry()
        runCurrent()

        assertThat(coordinator.entry.value).isEqualTo(
            NetworkedIdentityEntry(linkAvailable = true, accountEmail = "person@example.com", needsEmail = false)
        )
        assertThat(coordinator.entry.value.offersReuse).isTrue()
    }

    @Test
    fun `without an email Link is offered without an account and the sheet asks for one`() =
        runNetworkedIdentityScenario {
            coordinator.refreshEntry()
            runCurrent()

            assertThat(coordinator.entry.value).isEqualTo(
                NetworkedIdentityEntry(linkAvailable = true, accountEmail = null, needsEmail = true)
            )
            assertThat(coordinator.entry.value.offersReuse).isTrue()
        }

    @Test
    fun `provided email with a Link account offers reuse after the lookup`() = runNetworkedIdentityScenario(
        config = niConfig(merchantEmail = "person@example.com")
    ) {
        assertThat(coordinator.entry.value.offersReuse).isFalse()

        coordinator.refreshEntry()
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
        coordinator.refreshEntry()
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
        coordinator.refreshEntry()
        runCurrent()

        assertThat(coordinator.entry.value.linkAvailable).isFalse()
        assertThat(coordinator.entry.value.offersReuse).isFalse()
    }

    @Test
    fun `Link is configured once for the check and the attempt`() = runNetworkedIdentityScenario(
        config = niConfig(merchantEmail = "person@example.com")
    ) {
        coordinator.refreshEntry()
        runCurrent()
        linkSession.configureCalls.awaitItem().response.complete(Result.success(Unit))
        runCurrent()
        linkSession.lookupCalls.awaitItem().response.complete(Result.success(niAccount()))
        runCurrent()

        coordinator.startReuse()
        runCurrent()

        // No second configure call: the attempt goes straight to looking up the email.
        assertThat(linkSession.lookupCalls.awaitItem().email).isEqualTo("person@example.com")
        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.LookupPending)
    }

    @Test
    fun `sharing a document marks the verification as shared`() = runNetworkedIdentityScenario {
        assertThat(coordinator.shared.value).isFalse()
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
        assertThat(coordinator.shared.value).isTrue()
    }
}
