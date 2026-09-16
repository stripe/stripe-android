package com.stripe.android.identity.networked

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.identity.networked.NetworkedIdentityActionException.Reason
import com.stripe.android.identity.networking.models.Requirement
import org.junit.Test

internal class NetworkedIdentityActionTest {
    @Test
    fun `standalone flow cannot mint tokens even after selection`() = runNetworkedIdentityScenario {
        val viewModel = NetworkedIdentityViewModel(coordinator)
        assertThat(viewModel.supportsDocumentAttachment).isFalse()
        selectDocument()
        viewModel.continueWithSelectedDocument()
        runCurrent()
        assertThat(coordinator.state.value).isInstanceOf(NetworkedIdentityState.SelectDocument::class.java)
        repository.associationTokenCalls.expectNoEvents()
    }

    @Test
    fun `attachment requires an explicitly selected document`() = runNetworkedIdentityScenario(withActions = true) {
        coordinator.continueWithSelectedDocument()
        loadDocuments().response.complete(Result.success(listOf(niDocument())))
        runCurrent()
        coordinator.continueWithSelectedDocument()
        runCurrent()
        repository.associationTokenCalls.expectNoEvents()
        assertThat((coordinator.state.value as NetworkedIdentityState.SelectDocument).selectedDocumentId).isNull()
    }

    @Test
    fun `continue mints one token then attaches with latest credentials`() = runNetworkedIdentityScenario(
        withActions = true
    ) {
        val viewModel = NetworkedIdentityViewModel(coordinator)
        assertThat(viewModel.supportsDocumentAttachment).isTrue()
        selectDocument()
        repository.associationTokenCalls.expectNoEvents()
        coordinator.state.test {
            assertThat(awaitItem()).isInstanceOf(NetworkedIdentityState.SelectDocument::class.java)
            viewModel.continueWithSelectedDocument()
            assertThat(awaitItem()).isEqualTo(NetworkedIdentityState.AttachmentPending)
            viewModel.continueWithSelectedDocument()
            coordinator.useManualCapture()
            coordinator.selectDocument("another")
            runCurrent()
            val token = repository.associationTokenCalls.awaitItem()
            assertThat(token.documentId).isEqualTo("selected")
            assertThat(token.credentials.sessionClientSecret).isEqualTo("session_confirmed")
            assertThat(token.credentials.publishableKey).isEqualTo("pk_consumer")
            repository.associationTokenCalls.expectNoEvents()
            actions.skipCalls.expectNoEvents()
            token.response.complete(Result.success(NetworkedIdentityAssociationToken("reuse_token")))
            runCurrent()
            val attachment = actions.attachCalls.awaitItem()
            assertThat(attachment.associationToken).isEqualTo("reuse_token")
            assertThat(coordinator.state.value.toString()).doesNotContain("reuse_token")
            completions.expectNoEvents()
            attachment.response.complete(Result.success(niActionPageData()))
            runCurrent()
            assertThat(awaitItem()).isEqualTo(NetworkedIdentityState.Completed)
            val result = completions.awaitItem().getOrThrow()
            assertThat(result.id).isEqualTo(actions.verificationSessionId)
            assertThat(result.requirements.missings).containsExactly(Requirement.FACE)
            assertThat(result.submitted).isFalse()
            assertThat(repository.logoutCalls.awaitItem().credentials.sessionClientSecret)
                .isEqualTo("session_confirmed")
            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `completed action ignores further continue skip cancel and abandonment`() = runNetworkedIdentityScenario(
        withActions = true
    ) {
        beginAttachment().response.complete(Result.success(niActionPageData()))
        runCurrent()
        completions.awaitItem()
        repository.logoutCalls.awaitItem()
        coordinator.continueWithSelectedDocument()
        coordinator.useManualCapture()
        coordinator.cancel()
        coordinator.abandon()
        runCurrent()
        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.Completed)
        completions.expectNoEvents()
        cancellations.expectNoEvents()
        repository.associationTokenCalls.expectNoEvents()
        repository.logoutCalls.expectNoEvents()
    }

    @Test
    fun `continue rechecks document expiration before minting`() {
        var now = 100L
        runNetworkedIdentityScenario(withActions = true, currentTimeSeconds = { now }) {
            selectDocument()
            now = 200L
            coordinator.continueWithSelectedDocument()
            assertFallback(NetworkedIdentityFallbackReason.Unavailable)
            repository.associationTokenCalls.expectNoEvents()
            actions.attachCalls.expectNoEvents()
            actions.skipCalls.expectNoEvents()
        }
    }

    @Test
    fun `empty association token never attaches`() = assertTokenUnavailable(
        Result.success(NetworkedIdentityAssociationToken(""))
    )

    @Test
    fun `blank association token never attaches`() = assertTokenUnavailable(
        Result.success(NetworkedIdentityAssociationToken("  "))
    )

    @Test
    fun `failed token mint is sanitized and not retried`() = assertTokenUnavailable(
        Result.failure(IllegalStateException("sensitive_token_mint_error"))
    )

    @Test
    fun `attachment failure is sanitized without replay or skip`() = runNetworkedIdentityScenario(withActions = true) {
        beginAttachment().response.complete(Result.failure(IllegalStateException("sensitive_reuse_token")))
        assertCompletionFailure(Reason.AttachmentFailed)
        coordinator.continueWithSelectedDocument()
        coordinator.useManualCapture()
        runCurrent()
        repository.associationTokenCalls.expectNoEvents()
        actions.attachCalls.expectNoEvents()
        actions.skipCalls.expectNoEvents()
        fallbacks.expectNoEvents()
    }

    @Test
    fun `attachment response for another session is rejected`() = runNetworkedIdentityScenario(withActions = true) {
        beginAttachment().response.complete(Result.success(niActionPageData("vs_unrelated")))
        assertCompletionFailure(Reason.UnexpectedSession)
    }

    @Test
    fun `attachment unavailable falls back without skip`() = runNetworkedIdentityScenario(withActions = true) {
        beginAttachment().response.complete(Result.failure(niError("networked_identity_unavailable")))
        assertFallback(NetworkedIdentityFallbackReason.Unavailable)
        completions.expectNoEvents()
        repository.associationTokenCalls.expectNoEvents()
        actions.attachCalls.expectNoEvents()
        actions.skipCalls.expectNoEvents()
    }

    @Test
    fun `explicit manual capture skips once and returns server requirements`() = runNetworkedIdentityScenario(
        withActions = true
    ) {
        awaitOtp()
        coordinator.useManualCapture()
        coordinator.useManualCapture()
        coordinator.continueWithSelectedDocument()
        coordinator.submitOtp("123456")
        coordinator.resendOtp()
        runCurrent()
        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.SkipPending)
        val skip = actions.skipCalls.awaitItem()
        actions.skipCalls.expectNoEvents()
        repository.confirmCalls.expectNoEvents()
        repository.startCalls.expectNoEvents()
        skip.response.complete(Result.success(niActionPageData()))
        runCurrent()
        val result = completions.awaitItem().getOrThrow()
        assertThat(result.requirements.missings).containsExactly(Requirement.FACE)
        assertThat(result.submitted).isFalse()
        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.Completed)
        assertThat(repository.logoutCalls.awaitItem().credentials.sessionClientSecret).isEqualTo("session_started")
        fallbacks.expectNoEvents()
    }

    @Test
    fun `skip failure is sanitized without retry or fallback`() = runNetworkedIdentityScenario(withActions = true) {
        coordinator.useManualCapture()
        runCurrent()
        actions.skipCalls.awaitItem().response.complete(Result.failure(niError("networked_identity_unavailable")))
        assertCompletionFailure(Reason.SkipFailed, expectLogout = false)
        coordinator.useManualCapture()
        runCurrent()
        actions.skipCalls.expectNoEvents()
        fallbacks.expectNoEvents()
    }

    @Test
    fun `skip response for another session is rejected`() = runNetworkedIdentityScenario(withActions = true) {
        coordinator.useManualCapture()
        runCurrent()
        actions.skipCalls.awaitItem().response.complete(Result.success(niActionPageData("vs_unrelated")))
        assertCompletionFailure(Reason.UnexpectedSession, expectLogout = false)
    }

    @Test
    fun `automatic no-account fallback does not persist skip`() = runNetworkedIdentityScenario(withActions = true) {
        coordinator.submitEmail("person@example.com")
        runCurrent()
        repository.lookupCalls.awaitItem().response.complete(Result.success(NetworkedIdentityLookup.NotFound(null)))
        assertFallback(NetworkedIdentityFallbackReason.NoLinkAccount, expectLogout = false)
        actions.skipCalls.expectNoEvents()
        completions.expectNoEvents()
    }

    private fun assertTokenUnavailable(
        result: Result<NetworkedIdentityAssociationToken>
    ) = runNetworkedIdentityScenario(
        withActions = true
    ) {
        selectDocument()
        requestAssociationToken().response.complete(result)
        assertCompletionFailure(Reason.TokenUnavailable)
        actions.attachCalls.expectNoEvents()
        repository.associationTokenCalls.expectNoEvents()
    }

    private suspend fun NetworkedIdentityTestScenario.assertCompletionFailure(
        reason: Reason,
        expectLogout: Boolean = true
    ) {
        runCurrent()
        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.Completed)
        val error = completions.awaitItem().exceptionOrNull() as NetworkedIdentityActionException
        assertThat(error.reason).isEqualTo(reason)
        assertThat(error.message).isEqualTo(reason.name)
        assertThat(error.cause).isNull()
        if (expectLogout) repository.logoutCalls.awaitItem()
        fallbacks.expectNoEvents()
        cancellations.expectNoEvents()
    }
}
