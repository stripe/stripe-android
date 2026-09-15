package com.stripe.android.identity.networked

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import org.junit.Test

internal class NetworkedIdentityResendTest {
    @Test
    fun `resend before email lookup is ignored`() = runNetworkedIdentityScenario {
        coordinator.resendOtp()
        runCurrent()
        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.CollectEmail)
        repository.startCalls.expectNoEvents()
    }

    @Test
    fun `resend while initial SMS is pending is ignored`() = runNetworkedIdentityScenario {
        val start = startEmail()
        coordinator.resendOtp()
        runCurrent()
        repository.startCalls.expectNoEvents()
        start.response.complete(Result.success(niResponse()))
        runCurrent()
        assertThat((coordinator.state.value as NetworkedIdentityState.AwaitingOtp).otpGeneration).isEqualTo(1)
    }

    @Test
    fun `resend clears invalid OTP once and ignores pending resend and confirmation`() = runNetworkedIdentityScenario {
        awaitOtp()
        confirmOtp().response.complete(Result.failure(niError("consumer_verification_code_invalid")))
        runCurrent()
        coordinator.state.test {
            val invalid = awaitItem() as NetworkedIdentityState.AwaitingOtp
            assertThat(invalid.invalidCode).isTrue()
            coordinator.resendOtp()
            val pending = awaitItem() as NetworkedIdentityState.OtpResendPending
            assertThat(pending.redactedPhoneNumber).isEqualTo(invalid.redactedPhoneNumber)
            assertThat(pending.otpGeneration).isEqualTo(invalid.otpGeneration + 1)
            coordinator.resendOtp()
            coordinator.submitOtp("222222")
            runCurrent()
            val resend = repository.startCalls.awaitItem()
            assertThat(resend.isResendingSmsCode).isTrue()
            assertThat(resend.credentials.sessionClientSecret).isEqualTo("session_started")
            repository.startCalls.expectNoEvents()
            repository.confirmCalls.expectNoEvents()
            resend.response.complete(Result.success(niResponse()))
            runCurrent()
            val ready = awaitItem() as NetworkedIdentityState.AwaitingOtp
            assertThat(ready.invalidCode).isFalse()
            assertThat(ready.otpGeneration).isEqualTo(pending.otpGeneration)
            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `resend prefers a new ID and rotates credentials before confirmation`() = runNetworkedIdentityScenario {
        awaitOtp()
        val resend = resendOtp()
        assertThat(resend.authSessionSecrets).containsExactly("auth_lookup", "auth_started").inOrder()
        resend.response.complete(
            Result.success(
                niResponse(
                    clientSecret = "session_resent",
                    verificationSessions = listOf(niSms(), niSms("replacement")),
                    authSessionClientSecret = "auth_resent"
                )
            )
        )
        runCurrent()
        repository.documentCalls.expectNoEvents()
        val confirm = confirmOtp()
        assertThat(confirm.credentials.sessionClientSecret).isEqualTo("session_resent")
        assertThat(confirm.authSessionSecrets)
            .containsExactly("auth_lookup", "auth_started", "auth_resent").inOrder()
        confirm.response.complete(
            Result.success(
                niResponse(
                    clientSecret = "session_resent_confirmed",
                    verificationSessions = listOf(niSms(), niSms("replacement", VERIFIED))
                )
            )
        )
        runCurrent()
        val documents = repository.documentCalls.awaitItem()
        assertThat(documents.credentials.sessionClientSecret).isEqualTo("session_resent_confirmed")
        documents.response.complete(Result.success(listOf(niDocument())))
        runCurrent()
        assertThat(coordinator.state.value).isInstanceOf(NetworkedIdentityState.SelectDocument::class.java)
    }

    @Test
    fun `a replacement resend rejects confirmation of the previous active ID`() = runNetworkedIdentityScenario {
        awaitOtp()
        resendOtp().response.complete(
            Result.success(niResponse(verificationSessions = listOf(niSms(), niSms("replacement"))))
        )
        runCurrent()
        confirmOtp().response.complete(
            Result.success(niResponse(verificationSessions = listOf(niSms(state = VERIFIED), niSms("replacement"))))
        )
        assertFallback(NetworkedIdentityFallbackReason.Unavailable)
        repository.documentCalls.expectNoEvents()
    }

    @Test
    fun `resend can retain the active ID but must still confirm that ID`() = runNetworkedIdentityScenario {
        awaitOtp()
        resendOtp().response.complete(Result.success(niResponse(clientSecret = "session_retained")))
        runCurrent()
        repository.documentCalls.expectNoEvents()
        val confirm = confirmOtp()
        assertThat(confirm.credentials.sessionClientSecret).isEqualTo("session_retained")
        confirm.response.complete(Result.success(niResponse(verificationSessions = listOf(niSms(state = VERIFIED)))))
        runCurrent()
        repository.documentCalls.awaitItem().response.complete(Result.success(listOf(niDocument())))
        runCurrent()
        assertThat(coordinator.state.value).isInstanceOf(NetworkedIdentityState.SelectDocument::class.java)
    }

    @Test
    fun `resend cannot adopt an unrelated historical SMS ID`() = runNetworkedIdentityScenario {
        startEmail(niFound(niSession(verificationSessions = listOf(niSms("historical", VERIFIED)))))
            .response.complete(Result.success(niResponse()))
        runCurrent()
        resendOtp().response.complete(Result.success(niResponse(verificationSessions = listOf(niSms("historical")))))
        assertFallback(NetworkedIdentityFallbackReason.Unavailable)
        repository.confirmCalls.expectNoEvents()
    }

    @Test
    fun `resend remembers replaced IDs and cannot adopt them again`() = runNetworkedIdentityScenario {
        awaitOtp()
        resendOtp().response.complete(Result.success(niResponse(verificationSessions = listOf(niSms("replacement")))))
        runCurrent()
        resendOtp().response.complete(Result.success(niResponse()))
        assertFallback(NetworkedIdentityFallbackReason.Unavailable)
        repository.confirmCalls.expectNoEvents()
    }

    @Test
    fun `resend rejects ambiguous new IDs despite a matching active ID`() = assertResendRejected(
        listOf(niSms(), niSms("new_1"), niSms("new_2"))
    )

    @Test
    fun `resend rejects duplicate records for the retained active ID`() = assertResendRejected(listOf(niSms(), niSms()))

    @Test
    fun `resend rejects a missing SMS ID`() = assertResendRejected(listOf(niSms(id = null)))

    @Test
    fun `resend rejects an empty SMS ID`() = assertResendRejected(listOf(niSms(id = "")))

    @Test
    fun `resend cannot retain an already verified SMS session`() = assertResendRejected(listOf(niSms(state = VERIFIED)))

    @Test
    fun `resend cannot retain an active ID with an unknown verification type`() = assertResendRejected(
        listOf(niSms(type = NetworkedIdentityVerificationType.UNKNOWN))
    )

    @Test
    fun `resend session expiry requires explicit sign in and retains auth cookies`() = runNetworkedIdentityScenario {
        awaitOtp()
        resendOtp().response.complete(Result.failure(niError("consumer_session_expired")))
        runCurrent()
        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.ReauthenticationRequired)
        repository.lookupCalls.expectNoEvents()
        coordinator.resendOtp()
        coordinator.submitOtp("123456")
        runCurrent()
        repository.startCalls.expectNoEvents()
        repository.confirmCalls.expectNoEvents()
        coordinator.submitEmail("person@example.com")
        runCurrent()
        val lookup = repository.lookupCalls.awaitItem()
        assertThat(lookup.authSessionSecrets).containsExactly("auth_lookup", "auth_started").inOrder()
        lookup.response.complete(Result.success(NetworkedIdentityLookup.NotFound(null)))
        assertFallback(NetworkedIdentityFallbackReason.NoLinkAccount, expectLogout = false)
    }

    @Test
    fun `resend max attempts failure logs out and requests capture`() = runNetworkedIdentityScenario {
        awaitOtp()
        resendOtp().response.complete(Result.failure(niError("consumer_verification_max_attempts_exceeded")))
        assertFallback(NetworkedIdentityFallbackReason.Unavailable)
    }

    @Test
    fun `automatic expiry restart after resend still requires a new SMS ID`() = runNetworkedIdentityScenario {
        awaitOtp()
        resendOtp().response.complete(Result.success(niResponse()))
        runCurrent()
        confirmOtp().response.complete(Result.failure(niError("consumer_verification_expired")))
        runCurrent()
        val restart = repository.startCalls.awaitItem()
        assertThat(restart.isResendingSmsCode).isFalse()
        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.OtpStartPending)
        restart.response.complete(Result.success(niResponse()))
        assertFallback(NetworkedIdentityFallbackReason.Unavailable)
    }

    private fun assertResendRejected(
        sessions: List<NetworkedIdentityVerificationSession>
    ) = runNetworkedIdentityScenario {
        awaitOtp()
        resendOtp().response.complete(Result.success(niResponse(verificationSessions = sessions)))
        assertFallback(NetworkedIdentityFallbackReason.Unavailable)
        repository.confirmCalls.expectNoEvents()
        repository.documentCalls.expectNoEvents()
    }

    private companion object {
        val VERIFIED = NetworkedIdentityVerificationState.VERIFIED
    }
}
