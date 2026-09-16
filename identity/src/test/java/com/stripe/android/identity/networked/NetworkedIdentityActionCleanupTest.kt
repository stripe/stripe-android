package com.stripe.android.identity.networked

import com.google.common.truth.Truth.assertThat
import org.junit.Test

internal class NetworkedIdentityActionCleanupTest {
    @Test
    fun `cancel before token request runs prevents minting`() = runNetworkedIdentityScenario(withActions = true) {
        selectDocument()
        coordinator.continueWithSelectedDocument()
        coordinator.cancel()
        cancellations.awaitItem()
        runCurrent()
        repository.logoutCalls.awaitItem()
        repository.associationTokenCalls.expectNoEvents()
        actions.attachCalls.expectNoEvents()
        completions.expectNoEvents()
    }

    @Test
    fun `cancel during token mint ignores late token and prevents attachment`() = runNetworkedIdentityScenario(
        withActions = true
    ) {
        selectDocument()
        val token = requestAssociationToken()
        coordinator.cancel()
        cancellations.awaitItem()
        runCurrent()
        repository.logoutCalls.awaitItem()
        token.response.complete(Result.success(NetworkedIdentityAssociationToken("late_token")))
        runCurrent()
        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.Cancelled)
        actions.attachCalls.expectNoEvents()
        actions.skipCalls.expectNoEvents()
        completions.expectNoEvents()
    }

    @Test
    fun `abandon during token mint silently ignores late token`() = runNetworkedIdentityScenario(withActions = true) {
        selectDocument()
        val token = requestAssociationToken()
        coordinator.abandon()
        runCurrent()
        repository.logoutCalls.awaitItem()
        token.response.complete(Result.success(NetworkedIdentityAssociationToken("late_token")))
        runCurrent()
        actions.attachCalls.expectNoEvents()
        cancellations.expectNoEvents()
        completions.expectNoEvents()
    }

    @Test
    fun `cancel during attachment ignores late result without undo or skip`() = runNetworkedIdentityScenario(
        withActions = true
    ) {
        val attachment = beginAttachment()
        coordinator.cancel()
        cancellations.awaitItem()
        runCurrent()
        repository.logoutCalls.awaitItem()
        attachment.response.complete(Result.success(niActionPageData()))
        runCurrent()
        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.Cancelled)
        completions.expectNoEvents()
        actions.skipCalls.expectNoEvents()
        repository.logoutCalls.expectNoEvents()
    }

    @Test
    fun `cancel during skip ignores late server result`() = runNetworkedIdentityScenario(withActions = true) {
        coordinator.useManualCapture()
        runCurrent()
        val skip = actions.skipCalls.awaitItem()
        coordinator.cancel()
        cancellations.awaitItem()
        skip.response.complete(Result.success(niActionPageData()))
        runCurrent()
        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.Cancelled)
        completions.expectNoEvents()
        fallbacks.expectNoEvents()
    }

    @Test
    fun `skip pending cleans late lookup without starting SMS`() = runNetworkedIdentityScenario(withActions = true) {
        coordinator.submitEmail("person@example.com")
        runCurrent()
        val lookup = repository.lookupCalls.awaitItem()
        coordinator.useManualCapture()
        runCurrent()
        val skip = actions.skipCalls.awaitItem()
        lookup.response.complete(Result.success(niFound()))
        runCurrent()
        val logout = repository.logoutCalls.awaitItem()
        assertThat(logout.credentials.sessionClientSecret).isEqualTo("session_lookup")
        assertThat(logout.authSessionSecrets).containsExactly("auth_lookup")
        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.SkipPending)
        repository.startCalls.expectNoEvents()
        completions.expectNoEvents()
        skip.response.complete(Result.success(niActionPageData()))
        runCurrent()
        completions.awaitItem().getOrThrow()
        repository.logoutCalls.expectNoEvents()
    }

    @Test
    fun `skip pending logs out late rotated start credentials before skip resolves`() = runNetworkedIdentityScenario(
        withActions = true
    ) {
        val start = startEmail()
        coordinator.useManualCapture()
        runCurrent()
        val skip = actions.skipCalls.awaitItem()
        start.response.complete(Result.success(niResponse()))
        runCurrent()
        val lateLogout = repository.logoutCalls.awaitItem()
        assertThat(lateLogout.credentials.sessionClientSecret).isEqualTo("session_started")
        assertThat(lateLogout.authSessionSecrets).containsExactly("auth_lookup", "auth_started").inOrder()
        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.SkipPending)
        completions.expectNoEvents()
        skip.response.complete(Result.success(niActionPageData()))
        runCurrent()
        completions.awaitItem().getOrThrow()
        assertThat(repository.logoutCalls.awaitItem().credentials.sessionClientSecret).isEqualTo("session_lookup")
    }

    @Test
    fun `skip pending logs out late confirmation without listing documents`() = runNetworkedIdentityScenario(
        withActions = true
    ) {
        awaitOtp()
        val confirm = confirmOtp()
        coordinator.useManualCapture()
        runCurrent()
        val skip = actions.skipCalls.awaitItem()
        confirm.response.complete(
            Result.success(
                niResponse(
                    clientSecret = "late_confirmed",
                    verificationSessions = listOf(niSms(state = NetworkedIdentityVerificationState.VERIFIED)),
                    authSessionClientSecret = "auth_late"
                )
            )
        )
        runCurrent()
        val logout = repository.logoutCalls.awaitItem()
        assertThat(logout.credentials.sessionClientSecret).isEqualTo("late_confirmed")
        assertThat(logout.authSessionSecrets).containsExactly("auth_lookup", "auth_started", "auth_late").inOrder()
        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.SkipPending)
        repository.documentCalls.expectNoEvents()
        skip.response.complete(Result.success(niActionPageData()))
        runCurrent()
        completions.awaitItem().getOrThrow()
        assertThat(repository.logoutCalls.awaitItem().credentials.sessionClientSecret).isEqualTo("session_started")
    }

    @Test
    fun `skip pending ignores a late document list`() = runNetworkedIdentityScenario(withActions = true) {
        val documents = loadDocuments()
        coordinator.useManualCapture()
        runCurrent()
        val skip = actions.skipCalls.awaitItem()
        documents.response.complete(Result.success(listOf(niDocument())))
        runCurrent()
        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.SkipPending)
        completions.expectNoEvents()
        skip.response.complete(Result.success(niActionPageData()))
        runCurrent()
        completions.awaitItem().getOrThrow()
        repository.logoutCalls.awaitItem()
    }

    @Test
    fun `completed skip still cleans up late lookup credentials`() = runNetworkedIdentityScenario(withActions = true) {
        coordinator.submitEmail("person@example.com")
        runCurrent()
        val lookup = repository.lookupCalls.awaitItem()
        coordinator.useManualCapture()
        runCurrent()
        actions.skipCalls.awaitItem().response.complete(Result.success(niActionPageData()))
        runCurrent()
        completions.awaitItem().getOrThrow()
        lookup.response.complete(Result.success(niFound()))
        runCurrent()
        repository.logoutCalls.awaitItem()
        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.Completed)
        repository.startCalls.expectNoEvents()
        completions.expectNoEvents()
    }
}
