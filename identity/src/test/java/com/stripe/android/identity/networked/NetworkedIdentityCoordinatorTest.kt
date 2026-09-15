package com.stripe.android.identity.networked

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.identity.networking.models.NetworkedIdentityRoute
import org.junit.Test

@Suppress("LargeClass")
internal class NetworkedIdentityCoordinatorTest {

    @Test
    fun `nothing starts without an explicit start`() = runNetworkedIdentityScenario {
        coordinator.submitEmail("person@example.com")
        runCurrent()

        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.Idle)
    }

    @Test
    fun `start without a merchant publishable key falls back`() = runNetworkedIdentityScenario(
        config = niConfig(merchantPublishableKey = null)
    ) {
        coordinator.startReuse()

        assertFallback(NetworkedIdentityFallbackReason.Unavailable)
    }

    @Test
    fun `Link configuration failure falls back`() = runNetworkedIdentityScenario {
        coordinator.startReuse()
        runCurrent()
        val configure = linkSession.configureCalls.awaitItem()
        assertThat(configure.merchantPublishableKey).isEqualTo("pk_test_merchant")

        configure.response.complete(Result.failure(IllegalStateException("Link is not available")))

        assertFallback(NetworkedIdentityFallbackReason.Unavailable)
    }

    @Test
    fun `without a handed-in session or known email the user enters an email`() = runNetworkedIdentityScenario {
        startAndConfigure()

        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.CollectEmail)
    }

    @Test
    fun `known merchant email is looked up without asking for it`() = runNetworkedIdentityScenario(
        config = niConfig(merchantEmail = "merchant@example.com")
    ) {
        startAndConfigure()

        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.LookupPending)
        assertThat(linkSession.lookupCalls.awaitItem().email).isEqualTo("merchant@example.com")
    }

    @Test
    fun `invalid email is not looked up`() = runNetworkedIdentityScenario {
        startAndConfigure()

        coordinator.submitEmail("jane@example")
        runCurrent()

        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.CollectEmail)
    }

    @Test
    fun `handed-in session is not used before the user starts`() = runNetworkedIdentityScenario(
        handoff = niHandoff()
    ) {
        runCurrent()

        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.Idle)
    }

    @Test
    fun `verified handed-in session skips signing in and loads documents`() = runNetworkedIdentityScenario(
        handoff = niHandoff()
    ) {
        startAndConfigure()
        val restore = linkSession.restoreCalls.awaitItem()
        assertThat(restore.credentials).isEqualTo(
            NetworkedIdentityCredentials(publishableKey = "pk_consumer_handoff", sessionClientSecret = "handoff_secret")
        )

        restore.response.complete(Result.success(niAccount(isVerified = true)))
        runCurrent()

        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.DocumentsPending)
        assertThat(repository.documentCalls.awaitItem().credentials.sessionClientSecret).isEqualTo("session_secret")
    }

    @Test
    fun `unverified handed-in session sends a code without asking for the email`() = runNetworkedIdentityScenario(
        handoff = niHandoff()
    ) {
        startAndConfigure()
        linkSession.restoreCalls.awaitItem().response.complete(Result.success(niAccount(isVerified = false)))
        runCurrent()

        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.OtpStartPending)
        assertThat(linkSession.startVerificationCalls.awaitItem().isResend).isFalse()
    }

    @Test
    fun `expired handed-in session falls back to looking up its email`() = runNetworkedIdentityScenario(
        handoff = niHandoff()
    ) {
        startAndConfigure()
        linkSession.restoreCalls.awaitItem().response.complete(Result.failure(IllegalStateException("expired")))
        runCurrent()

        assertThat(linkSession.lookupCalls.awaitItem().email).isEqualTo("person@example.com")
    }

    @Test
    fun `reuse without a Link account falls back to capture`() = runNetworkedIdentityScenario {
        startAndConfigure()

        signInWithEmail(account = null)

        assertFallback(NetworkedIdentityFallbackReason.NoLinkAccount)
    }

    @Test
    fun `code is sent and confirmed before documents load`() = runNetworkedIdentityScenario {
        startAndConfigure()
        signInWithEmail()
        completeCodeSent()
        assertThat(coordinator.state.value).isEqualTo(
            NetworkedIdentityState.AwaitingOtp(
                redactedPhoneNumber = "(***) ***-1234",
                invalidCode = false,
                otpGeneration = 1,
            )
        )

        confirmCode()

        assertThat(repository.documentCalls.awaitItem().credentials).isEqualTo(
            NetworkedIdentityCredentials(publishableKey = "pk_consumer", sessionClientSecret = "session_secret")
        )
    }

    @Test
    fun `malformed code is not submitted`() = runNetworkedIdentityScenario {
        startAndConfigure()
        signInWithEmail()
        completeCodeSent()

        coordinator.submitOtp("12a456")
        coordinator.submitOtp("123")
        runCurrent()

        assertThat(coordinator.state.value).isInstanceOf(NetworkedIdentityState.AwaitingOtp::class.java)
    }

    @Test
    fun `invalid code keeps the user on the code step`() = runNetworkedIdentityScenario {
        startAndConfigure()
        signInWithEmail()
        completeCodeSent()
        coordinator.submitOtp("123456")
        runCurrent()

        linkSession.confirmVerificationCalls.awaitItem().response.complete(
            Result.failure(consumerError("consumer_verification_code_invalid"))
        )
        runCurrent()

        assertThat(coordinator.state.value).isEqualTo(
            NetworkedIdentityState.AwaitingOtp(
                redactedPhoneNumber = "(***) ***-1234",
                invalidCode = true,
                otpGeneration = 1,
            )
        )
    }

    @Test
    fun `expired code sends a new one`() = runNetworkedIdentityScenario {
        startAndConfigure()
        signInWithEmail()
        completeCodeSent()
        coordinator.submitOtp("123456")
        runCurrent()

        linkSession.confirmVerificationCalls.awaitItem().response.complete(
            Result.failure(consumerError("consumer_verification_expired"))
        )
        runCurrent()
        assertThat(linkSession.startVerificationCalls.awaitItem().isResend).isFalse()
    }

    @Test
    fun `expired session asks to sign in again`() = runNetworkedIdentityScenario {
        startAndConfigure()
        signInWithEmail()
        completeCodeSent()
        coordinator.submitOtp("123456")
        runCurrent()

        linkSession.confirmVerificationCalls.awaitItem().response.complete(
            Result.failure(consumerError("consumer_session_expired"))
        )
        runCurrent()

        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.ReauthenticationRequired)
    }

    @Test
    fun `resend requests a new code`() = runNetworkedIdentityScenario {
        startAndConfigure()
        signInWithEmail()
        completeCodeSent()

        coordinator.resendOtp()
        runCurrent()
        val resend = linkSession.startVerificationCalls.awaitItem()
        assertThat(resend.isResend).isTrue()
        resend.response.complete(Result.success(niAccount()))
        runCurrent()

        assertThat((coordinator.state.value as NetworkedIdentityState.AwaitingOtp).otpGeneration).isEqualTo(2)
    }

    @Test
    fun `single eligible document is preselected but not shared`() = runNetworkedIdentityScenario {
        reachDocuments(listOf(niDocument("document_1")))

        assertThat(coordinator.state.value).isEqualTo(
            NetworkedIdentityState.SelectDocument(listOf(niDocument("document_1")), selectedDocumentId = "document_1")
        )
    }

    @Test
    fun `several eligible documents need a selection before sharing`() = runNetworkedIdentityScenario {
        reachDocuments(listOf(niDocument("document_1"), niDocument("document_2")))
        assertThat((coordinator.state.value as NetworkedIdentityState.SelectDocument).selectedDocumentId).isNull()

        coordinator.shareSelectedDocument()
        runCurrent()
        coordinator.selectDocument("document_2")

        assertThat((coordinator.state.value as NetworkedIdentityState.SelectDocument).selectedDocumentId)
            .isEqualTo("document_2")
    }

    @Test
    fun `sharing attaches the selected document and waits for continue`() = runNetworkedIdentityScenario {
        reachDocuments()

        coordinator.shareSelectedDocument()
        runCurrent()
        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.SharingDocument(niDocument()))
        val token = repository.tokenCalls.awaitItem()
        assertThat(token.documentId).isEqualTo("document_1")
        token.response.complete(Result.success(NetworkedIdentityAssociationToken("token_1")))
        runCurrent()
        val attach = actions.attachCalls.awaitItem()
        assertThat(attach.associationToken).isEqualTo("token_1")
        attach.response.complete(Result.success(niActionPageData()))
        runCurrent()

        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.DocumentShared(niDocument()))
        outcomes.expectNoEvents()

        coordinator.continueAfterSuccess()

        assertThat(outcomes.awaitItem())
            .isEqualTo(NetworkedIdentityOutcome.DocumentShared(niDocument(), niActionPageData()))
        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.Idle)
    }

    @Test
    fun `a failed attach falls back without replaying the token or skipping`() = runNetworkedIdentityScenario {
        reachDocuments()

        coordinator.shareSelectedDocument()
        runCurrent()
        repository.tokenCalls.awaitItem().response.complete(
            Result.success(NetworkedIdentityAssociationToken("token_1"))
        )
        runCurrent()
        actions.attachCalls.awaitItem().response.complete(Result.failure(IllegalStateException("Attach failed.")))

        assertFallback(NetworkedIdentityFallbackReason.Unavailable)
    }

    @Test
    fun `no eligible documents falls back to capture`() = runNetworkedIdentityScenario {
        reachDocuments(listOf(niDocument().copy(liveCaptured = false)))

        assertFallback(NetworkedIdentityFallbackReason.NoReusableDocuments)
    }

    @Test
    fun `save for a new account collects a phone number and signs up`() = runNetworkedIdentityScenario(
        config = niConfig(route = NetworkedIdentityRoute.Save)
    ) {
        startAndConfigure(NetworkedIdentityMode.Save)
        signInWithEmail(account = null)
        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.CollectPhone("person@example.com"))

        coordinator.submitPhone(phoneNumber = "+15555551234", country = "US")
        runCurrent()
        val signUp = linkSession.signUpCalls.awaitItem()
        assertThat(signUp.email).isEqualTo("person@example.com")
        assertThat(signUp.phoneNumber).isEqualTo("+15555551234")
        assertThat(signUp.country).isEqualTo("US")
        signUp.response.complete(Result.success(niAccount(isVerified = true)))
        runCurrent()
        val saveToken = repository.saveTokenCalls.awaitItem()
        assertThat(saveToken.credentials.sessionClientSecret).isEqualTo("session_secret")
        assertThat(saveToken.verificationSessionId).isEqualTo("vs_target")
        saveToken.response.complete(Result.success(NetworkedIdentityAssociationToken("save_token")))
        runCurrent()
        val prepare = actions.prepareSaveCalls.awaitItem()
        assertThat(prepare.associationToken).isEqualTo("save_token")
        prepare.response.complete(Result.success(niActionPageData()))
        runCurrent()

        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.Saved)
        assertThat(coordinator.saved.value).isTrue()
        outcomes.expectNoEvents()

        coordinator.continueAfterSuccess()

        assertThat(outcomes.awaitItem()).isEqualTo(NetworkedIdentityOutcome.Saved)
    }

    @Test
    fun `save for an unverified existing account confirms a code first`() = runNetworkedIdentityScenario(
        config = niConfig(route = NetworkedIdentityRoute.Save)
    ) {
        startAndConfigure(NetworkedIdentityMode.Save)
        signInWithEmail()
        completeCodeSent()
        confirmCode()

        repository.saveTokenCalls.awaitItem()
        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.SavePending)
    }

    @Test
    fun `a failed save keeps the sheet open until it is closed`() = runNetworkedIdentityScenario(
        config = niConfig(route = NetworkedIdentityRoute.Save)
    ) {
        startAndConfigure(NetworkedIdentityMode.Save)
        signInWithEmail()
        completeCodeSent()
        confirmCode()

        repository.saveTokenCalls.awaitItem().response.complete(Result.failure(IllegalStateException("Save failed.")))
        runCurrent()

        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.SaveFailed("Save failed."))
        outcomes.expectNoEvents()

        coordinator.cancel()
        runCurrent()

        assertThat(outcomes.awaitItem()).isEqualTo(NetworkedIdentityOutcome.Cancelled)
    }

    @Test
    fun `cancel reports cancellation and a new attempt can start`() = runNetworkedIdentityScenario {
        startAndConfigure()

        coordinator.cancel()
        runCurrent()
        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.Cancelled)
        assertThat(outcomes.awaitItem()).isEqualTo(NetworkedIdentityOutcome.Cancelled)

        coordinator.startReuse()
        runCurrent()

        // Link stays configured, and the Link session is never logged out.
        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.CollectEmail)
    }

    @Test
    fun `responses from a cancelled attempt are ignored`() = runNetworkedIdentityScenario {
        startAndConfigure()
        coordinator.submitEmail("person@example.com")
        runCurrent()
        val lookup = linkSession.lookupCalls.awaitItem()
        coordinator.cancel()
        runCurrent()
        outcomes.awaitItem()

        lookup.response.complete(Result.success(niAccount(isVerified = true)))
        runCurrent()

        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.Cancelled)
    }

    @Test
    fun `manual capture reports a fallback`() = runNetworkedIdentityScenario {
        startAndConfigure()

        coordinator.useManualCapture()

        assertFallback(NetworkedIdentityFallbackReason.UserSelectedManualCapture)
    }

    @Test
    fun `abandon stops without reporting an outcome`() = runNetworkedIdentityScenario {
        startAndConfigure()

        coordinator.abandon()
        runCurrent()

        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.Cancelled)
        outcomes.expectNoEvents()
    }

    private fun consumerError(code: String) = InvalidRequestException(stripeError = StripeError(code = code))
}
