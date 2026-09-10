package com.stripe.android.identity.networked

import com.google.common.truth.Truth.assertThat
import org.junit.Test

internal class NetworkedIdentityCleanupTest {
    @Test
    fun `consumer secret updates and auth secrets remain nonempty unique and ordered`() = runNetworkedIdentityScenario(
        authSessionSecrets = listOf("", "seed", "seed", " ")
    ) {
        coordinator.submitEmail("person@example.com")
        runCurrent()
        val lookup = repository.lookupCalls.awaitItem()
        assertThat(lookup.authSessionSecrets).containsExactly("seed")
        lookup.response.complete(Result.success(niFound(authSessionClientSecret = "seed")))
        runCurrent()
        val start = repository.startCalls.awaitItem()
        assertThat(start.authSessionSecrets).containsExactly("seed")
        start.response.complete(Result.success(niResponse(authSessionClientSecret = "")))
        runCurrent()
        val confirm = confirmOtp()
        assertThat(confirm.credentials.sessionClientSecret).isEqualTo("session_started")
        assertThat(confirm.authSessionSecrets).containsExactly("seed")
        confirm.response.complete(
            Result.success(
                niResponse(
                    clientSecret = "session_confirmed",
                    verificationSessions = listOf(niSms(state = NetworkedIdentityVerificationState.VERIFIED)),
                    authSessionClientSecret = "auth_confirmed"
                )
            )
        )
        runCurrent()
        val documents = repository.documentCalls.awaitItem()
        assertThat(documents.credentials.sessionClientSecret).isEqualTo("session_confirmed")
        documents.response.complete(Result.success(listOf(niDocument())))
        runCurrent()
        coordinator.cancel()
        cancellations.awaitItem()
        runCurrent()
        val logout = repository.logoutCalls.awaitItem()
        assertThat(logout.authSessionSecrets).containsExactly("seed", "auth_confirmed").inOrder()
        assertThat(logout.credentials.sessionClientSecret).isEqualTo("session_confirmed")
    }

    @Test
    fun `cancel clears selection and metadata and invokes host once`() = runNetworkedIdentityScenario {
        loadDocuments().response.complete(Result.success(listOf(niDocument())))
        runCurrent()
        coordinator.selectDocument("document_1")
        assertThat((coordinator.state.value as NetworkedIdentityState.SelectDocument).selectedDocumentId)
            .isEqualTo("document_1")
        coordinator.cancel()
        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.Cancelled)
        cancellations.awaitItem()
        coordinator.cancel()
        coordinator.useManualCapture()
        coordinator.selectDocument("document_1")
        coordinator.submitEmail("other@example.com")
        runCurrent()
        repository.logoutCalls.awaitItem()
        cancellations.expectNoEvents()
        fallbacks.expectNoEvents()
        repository.lookupCalls.expectNoEvents()
        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.Cancelled)
    }

    @Test
    fun `manual capture reports explicit reason and invokes host once`() = runNetworkedIdentityScenario {
        awaitOtp()
        coordinator.useManualCapture()
        assertFallback(NetworkedIdentityFallbackReason.UserSelectedManualCapture)
        coordinator.useManualCapture()
        coordinator.cancel()
        runCurrent()
        cancellations.expectNoEvents()
        fallbacks.expectNoEvents()
        repository.logoutCalls.expectNoEvents()
    }

    @Test
    fun `cancellation suppresses a queued obsolete fallback callback`() = runNetworkedIdentityScenario {
        awaitOtp()
        coordinator.useManualCapture()
        assertThat(coordinator.state.value).isInstanceOf(NetworkedIdentityState.FullCaptureFallback::class.java)
        coordinator.cancel()
        cancellations.awaitItem()
        runCurrent()
        repository.logoutCalls.awaitItem()
        fallbacks.expectNoEvents()
        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.Cancelled)
    }

    @Test
    fun `abandonment suppresses queued fallback without calling host`() = runNetworkedIdentityScenario {
        awaitOtp()
        coordinator.useManualCapture()
        coordinator.abandon()
        runCurrent()
        repository.logoutCalls.awaitItem()
        fallbacks.expectNoEvents()
        cancellations.expectNoEvents()
        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.Cancelled)
    }

    @Test
    fun `late lookup after cancellation logs out returned credentials and auth secret`() = runNetworkedIdentityScenario(
        authSessionSecrets = listOf("seed")
    ) {
        coordinator.submitEmail("person@example.com")
        runCurrent()
        val lookup = repository.lookupCalls.awaitItem()
        coordinator.cancel()
        cancellations.awaitItem()
        lookup.response.complete(Result.success(niFound()))
        runCurrent()
        val logout = repository.logoutCalls.awaitItem()
        assertThat(logout.credentials.sessionClientSecret).isEqualTo("session_lookup")
        assertThat(logout.credentials.publishableKey).isEqualTo("pk_consumer")
        assertThat(logout.authSessionSecrets).containsExactly("seed", "auth_lookup").inOrder()
        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.Cancelled)
        repository.startCalls.expectNoEvents()
    }

    @Test
    fun `late start after cancellation logs out rotated credentials`() = runNetworkedIdentityScenario {
        val start = startEmail()
        coordinator.cancel()
        cancellations.awaitItem()
        runCurrent()
        assertThat(repository.logoutCalls.awaitItem().credentials.sessionClientSecret).isEqualTo("session_lookup")
        start.response.complete(Result.success(niResponse()))
        runCurrent()
        val lateLogout = repository.logoutCalls.awaitItem()
        assertThat(lateLogout.credentials.sessionClientSecret).isEqualTo("session_started")
        assertThat(lateLogout.authSessionSecrets).containsExactly("auth_lookup", "auth_started").inOrder()
        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.Cancelled)
    }

    @Test
    fun `late confirmation after manual capture logs out rotated credentials`() = runNetworkedIdentityScenario {
        awaitOtp()
        val confirm = confirmOtp()
        coordinator.useManualCapture()
        assertFallback(NetworkedIdentityFallbackReason.UserSelectedManualCapture)
        confirm.response.complete(
            Result.success(
                niResponse(
                    clientSecret = "session_confirmed",
                    verificationSessions = listOf(niSms(state = NetworkedIdentityVerificationState.VERIFIED)),
                    authSessionClientSecret = "auth_confirmed"
                )
            )
        )
        runCurrent()
        val logout = repository.logoutCalls.awaitItem()
        assertThat(logout.credentials.sessionClientSecret).isEqualTo("session_confirmed")
        assertThat(logout.authSessionSecrets)
            .containsExactly("auth_lookup", "auth_started", "auth_confirmed").inOrder()
        repository.documentCalls.expectNoEvents()
        fallbacks.expectNoEvents()
        assertThat(coordinator.state.value).isInstanceOf(NetworkedIdentityState.FullCaptureFallback::class.java)
    }

    @Test
    fun `late documents cannot reopen a cancelled flow`() = runNetworkedIdentityScenario {
        val documents = loadDocuments()
        coordinator.cancel()
        cancellations.awaitItem()
        runCurrent()
        repository.logoutCalls.awaitItem()
        documents.response.complete(Result.success(listOf(niDocument())))
        runCurrent()
        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.Cancelled)
        repository.logoutCalls.expectNoEvents()
    }

    @Test
    fun `late failed lookup does not call host or logout`() = runNetworkedIdentityScenario {
        coordinator.submitEmail("person@example.com")
        runCurrent()
        val lookup = repository.lookupCalls.awaitItem()
        coordinator.abandon()
        lookup.response.complete(Result.failure(IllegalStateException()))
        runCurrent()
        repository.logoutCalls.expectNoEvents()
        cancellations.expectNoEvents()
        fallbacks.expectNoEvents()
    }

    @Test
    fun `renderable OTP state contains only redacted display metadata`() = runNetworkedIdentityScenario {
        awaitOtp()
        val display = coordinator.state.value.toString()
        assertThat(display).contains("(***) ***-1234")
        assertThat(display).doesNotContain("person@example.com")
        assertThat(display).doesNotContain("+15555551234")
        assertThat(display).doesNotContain("session_started")
        assertThat(display).doesNotContain("auth_started")
        val confirm = confirmOtp()
        assertThat(coordinator.state.value.toString()).doesNotContain(confirm.code)
        confirm.response.complete(Result.failure(niError("consumer_verification_code_invalid")))
        runCurrent()
    }
}
