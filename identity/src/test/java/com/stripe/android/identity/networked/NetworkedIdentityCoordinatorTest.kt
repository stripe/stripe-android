package com.stripe.android.identity.networked

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.InvalidRequestException
import org.junit.Test

internal class NetworkedIdentityCoordinatorTest {
    @Test
    fun `blank email does not start lookup`() = runNetworkedIdentityScenario {
        coordinator.submitEmail("  ")
        runCurrent()
        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.CollectEmail)
        repository.lookupCalls.expectNoEvents()
    }

    @Test
    fun `invalid email does not start lookup`() = runNetworkedIdentityScenario {
        coordinator.submitEmail("person@example")
        runCurrent()
        repository.lookupCalls.expectNoEvents()
    }

    @Test
    fun `pending lookup ignores duplicate email submission`() = runNetworkedIdentityScenario {
        coordinator.state.test {
            assertThat(awaitItem()).isEqualTo(NetworkedIdentityState.CollectEmail)
            coordinator.submitEmail(" person@example.com ")
            assertThat(awaitItem()).isEqualTo(NetworkedIdentityState.LookupPending)
            coordinator.submitEmail("other@example.com")
            runCurrent()
            val lookup = repository.lookupCalls.awaitItem()
            assertThat(lookup.email).isEqualTo("person@example.com")
            repository.lookupCalls.expectNoEvents()
            lookup.response.complete(Result.success(NetworkedIdentityLookup.NotFound(null)))
            runCurrent()
            assertThat(awaitItem()).isEqualTo(
                NetworkedIdentityState.FullCaptureFallback(NetworkedIdentityFallbackReason.NoLinkAccount)
            )
            assertThat(fallbacks.awaitItem()).isEqualTo(NetworkedIdentityFallbackReason.NoLinkAccount)
            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `lookup failure requests unavailable fallback`() = runNetworkedIdentityScenario {
        coordinator.submitEmail("person@example.com")
        runCurrent()
        repository.lookupCalls.awaitItem().response.complete(Result.failure(IllegalStateException()))
        assertFallback(NetworkedIdentityFallbackReason.Unavailable, expectLogout = false)
    }

    @Test
    fun `historical verified SMS still requires a fresh SMS`() = runNetworkedIdentityScenario {
        val start = startEmail(niFound(niSession(verificationSessions = listOf(niSms("old", VERIFIED)))))
        assertThat(start.credentials.sessionClientSecret).isEqualTo("session_lookup")
        assertThat(start.credentials.publishableKey).isEqualTo("pk_consumer")
        assertThat(start.locale).isEqualTo("en-US")
        assertThat(start.accountPhoneNumber).isNull()
        repository.documentCalls.expectNoEvents()
        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.OtpStartPending)
        start.response.complete(
            Result.success(niResponse(verificationSessions = listOf(niSms("old", VERIFIED), niSms())))
        )
        runCurrent()
        assertThat(coordinator.state.value).isEqualTo(
            NetworkedIdentityState.AwaitingOtp("(***) ***-1234", false, 1)
        )
        repository.documentCalls.expectNoEvents()
    }

    @Test
    fun `start without an SMS ID fails closed`() = assertStartRejected(listOf(niSms(id = null)))

    @Test
    fun `start with an empty SMS ID fails closed`() = assertStartRejected(listOf(niSms(id = "")))

    @Test
    fun `start with a recycled SMS ID fails closed`() = assertStartRejected(
        sessions = listOf(niSms("historical")),
        knownSessions = listOf(niSms("historical", VERIFIED))
    )

    @Test
    fun `start with ambiguous fresh SMS IDs fails closed`() = assertStartRejected(
        listOf(niSms("fresh_1"), niSms("fresh_2"))
    )

    @Test
    fun `start with duplicate fresh SMS records fails closed`() = assertStartRejected(listOf(niSms(), niSms()))

    @Test
    fun `unknown verification type cannot authenticate`() = assertStartRejected(
        listOf(niSms(type = NetworkedIdentityVerificationType.UNKNOWN))
    )

    @Test
    fun `unknown verification state cannot authenticate`() = assertStartRejected(
        listOf(niSms(state = NetworkedIdentityVerificationState.UNKNOWN))
    )

    @Test
    fun `start with a blank rotated secret fails closed`() = runNetworkedIdentityScenario {
        startEmail().response.complete(Result.success(niResponse(clientSecret = "")))
        assertFallback(NetworkedIdentityFallbackReason.Unavailable)
    }

    @Test
    fun `lookup with a blank consumer key fails closed`() = runNetworkedIdentityScenario {
        coordinator.submitEmail("person@example.com")
        runCurrent()
        repository.lookupCalls.awaitItem().response.complete(Result.success(niFound().copy(publishableKey = "")))
        assertFallback(NetworkedIdentityFallbackReason.Unavailable, expectLogout = false)
        repository.startCalls.expectNoEvents()
    }

    @Test
    fun `wrong verified ID cannot load documents`() = runNetworkedIdentityScenario {
        awaitOtp()
        confirmOtp().response.complete(
            Result.success(niResponse(verificationSessions = listOf(niSms("historical", VERIFIED), niSms())))
        )
        assertFallback(NetworkedIdentityFallbackReason.Unavailable)
        repository.documentCalls.expectNoEvents()
    }

    @Test
    fun `verified active ID of wrong type cannot load documents`() = runNetworkedIdentityScenario {
        awaitOtp()
        confirmOtp().response.complete(
            Result.success(
                niResponse(
                    verificationSessions = listOf(
                        niSms(state = VERIFIED, type = NetworkedIdentityVerificationType.EMAIL)
                    )
                )
            )
        )
        assertFallback(NetworkedIdentityFallbackReason.Unavailable)
        repository.documentCalls.expectNoEvents()
    }

    @Test
    fun `invalid OTP length does not submit`() = runNetworkedIdentityScenario {
        awaitOtp()
        coordinator.submitOtp("12345")
        runCurrent()
        repository.confirmCalls.expectNoEvents()
        assertThat(coordinator.state.value).isInstanceOf(NetworkedIdentityState.AwaitingOtp::class.java)
    }

    @Test
    fun `nondigit OTP does not submit`() = runNetworkedIdentityScenario {
        awaitOtp()
        coordinator.submitOtp("12345a")
        runCurrent()
        repository.confirmCalls.expectNoEvents()
    }

    @Test
    fun `pending confirmation ignores duplicate submissions`() = runNetworkedIdentityScenario {
        awaitOtp()
        val confirm = confirmOtp()
        coordinator.submitOtp("654321")
        runCurrent()
        repository.confirmCalls.expectNoEvents()
        assertThat(confirm.code).isEqualTo("123456")
        confirm.response.complete(Result.failure(niError("consumer_verification_code_invalid")))
        runCurrent()
    }

    @Test
    fun `invalid code returns editable OTP and permits retry`() = runNetworkedIdentityScenario {
        awaitOtp()
        coordinator.state.test {
            val initial = awaitItem() as NetworkedIdentityState.AwaitingOtp
            coordinator.submitOtp("111111")
            assertThat(awaitItem()).isInstanceOf(NetworkedIdentityState.OtpConfirmPending::class.java)
            runCurrent()
            repository.confirmCalls.awaitItem().response.complete(
                Result.failure(niError("consumer_verification_code_invalid"))
            )
            runCurrent()
            val invalid = awaitItem() as NetworkedIdentityState.AwaitingOtp
            assertThat(invalid.invalidCode).isTrue()
            assertThat(invalid.otpGeneration).isEqualTo(initial.otpGeneration)
            coordinator.submitOtp("222222")
            assertThat(awaitItem()).isInstanceOf(NetworkedIdentityState.OtpConfirmPending::class.java)
            runCurrent()
            val retry = repository.confirmCalls.awaitItem()
            assertThat(retry.code).isEqualTo("222222")
            retry.response.complete(Result.failure(niError("consumer_verification_code_invalid")))
            runCurrent()
            assertThat(awaitItem()).isEqualTo(invalid)
            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `expired verification restarts SMS and changes OTP generation`() = runNetworkedIdentityScenario {
        awaitOtp()
        confirmOtp().response.complete(Result.failure(niError("consumer_verification_expired")))
        runCurrent()
        val restart = repository.startCalls.awaitItem()
        assertThat(restart.credentials.sessionClientSecret).isEqualTo("session_started")
        restart.response.complete(Result.success(niResponse(verificationSessions = listOf(niSms("replacement")))))
        runCurrent()
        assertThat((coordinator.state.value as NetworkedIdentityState.AwaitingOtp).otpGeneration).isEqualTo(2)
    }

    @Test
    fun `expired verification restart rejects the previous active ID`() = runNetworkedIdentityScenario {
        awaitOtp()
        confirmOtp().response.complete(Result.failure(niError("consumer_verification_expired")))
        runCurrent()
        repository.startCalls.awaitItem().response.complete(Result.success(niResponse()))
        assertFallback(NetworkedIdentityFallbackReason.Unavailable)
    }

    @Test
    fun `start session expiry requires explicit email sign in again`() = runNetworkedIdentityScenario {
        startEmail().response.complete(Result.failure(niError("consumer_session_expired")))
        runCurrent()
        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.ReauthenticationRequired)
        coordinator.submitOtp("123456")
        runCurrent()
        repository.confirmCalls.expectNoEvents()
        coordinator.cancel()
        cancellations.awaitItem()
        runCurrent()
        repository.logoutCalls.expectNoEvents()
    }

    @Test
    fun `confirm session expiry clears credentials and retains auth secrets`() = runNetworkedIdentityScenario {
        awaitOtp()
        confirmOtp().response.complete(Result.failure(niError("consumer_session_expired")))
        runCurrent()
        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.ReauthenticationRequired)
        coordinator.submitEmail("new@example.com")
        runCurrent()
        val lookup = repository.lookupCalls.awaitItem()
        assertThat(lookup.authSessionSecrets).containsExactly("auth_lookup", "auth_started").inOrder()
        lookup.response.complete(Result.success(niFound()))
        runCurrent()
        repository.startCalls.awaitItem().response.complete(Result.success(niResponse()))
        runCurrent()
        assertThat((coordinator.state.value as NetworkedIdentityState.AwaitingOtp).otpGeneration).isEqualTo(2)
    }

    @Test
    fun `maximum verification attempts requests ordinary capture`() = runNetworkedIdentityScenario {
        awaitOtp()
        confirmOtp().response.complete(Result.failure(niError("consumer_verification_max_attempts_exceeded")))
        assertFallback(NetworkedIdentityFallbackReason.Unavailable)
    }

    @Test
    fun `unrecognized confirmation failure requests unavailable fallback`() = runNetworkedIdentityScenario {
        awaitOtp()
        confirmOtp().response.complete(Result.failure(IllegalStateException()))
        assertFallback(NetworkedIdentityFallbackReason.Unavailable)
    }

    @Test
    fun `confirmation rotates secret before loading eligible documents in order`() = runNetworkedIdentityScenario {
        val list = loadDocuments()
        assertThat(list.credentials.sessionClientSecret).isEqualTo("session_confirmed")
        list.response.complete(
            Result.success(
                listOf(
                    niDocument("expired").copy(expirationDate = 100),
                    niDocument("second"),
                    niDocument("unknown").copy(documentType = NetworkedIdentityDocumentType.UNKNOWN),
                    niDocument("first")
                )
            )
        )
        runCurrent()
        val state = coordinator.state.value as NetworkedIdentityState.SelectDocument
        assertThat(state.documents.map { it.id }).containsExactly("second", "first").inOrder()
        assertThat(state.selectedDocumentId).isNull()
    }

    @Test
    fun `selection can change without association or verification completion`() = runNetworkedIdentityScenario {
        loadDocuments().response.complete(Result.success(listOf(niDocument("one"), niDocument("two"))))
        runCurrent()
        coordinator.selectDocument("one")
        assertThat((coordinator.state.value as NetworkedIdentityState.SelectDocument).selectedDocumentId)
            .isEqualTo("one")
        coordinator.selectDocument("missing")
        assertThat((coordinator.state.value as NetworkedIdentityState.SelectDocument).selectedDocumentId)
            .isEqualTo("one")
        coordinator.selectDocument("two")
        assertThat((coordinator.state.value as NetworkedIdentityState.SelectDocument).selectedDocumentId)
            .isEqualTo("two")
        repository.unsupportedCalls.expectNoEvents()
        fallbacks.expectNoEvents()
        cancellations.expectNoEvents()
    }

    @Test
    fun `no eligible documents requests ordinary capture`() = runNetworkedIdentityScenario {
        loadDocuments().response.complete(Result.success(listOf(niDocument().copy(liveCaptured = null))))
        assertFallback(NetworkedIdentityFallbackReason.NoReusableDocuments)
    }

    @Test
    fun `document list failure requests unavailable fallback`() = runNetworkedIdentityScenario {
        loadDocuments().response.complete(Result.failure(IllegalStateException()))
        assertFallback(NetworkedIdentityFallbackReason.Unavailable)
    }

    private fun assertStartRejected(
        sessions: List<NetworkedIdentityVerificationSession>,
        knownSessions: List<NetworkedIdentityVerificationSession> = emptyList()
    ) = runNetworkedIdentityScenario {
        startEmail(niFound(niSession(verificationSessions = knownSessions))).response.complete(
            Result.success(niResponse(verificationSessions = sessions))
        )
        assertFallback(NetworkedIdentityFallbackReason.Unavailable)
        repository.confirmCalls.expectNoEvents()
        repository.documentCalls.expectNoEvents()
    }

    private companion object {
        val VERIFIED = NetworkedIdentityVerificationState.VERIFIED
    }
}

internal fun niError(code: String) = InvalidRequestException(stripeError = StripeError(code = code))
