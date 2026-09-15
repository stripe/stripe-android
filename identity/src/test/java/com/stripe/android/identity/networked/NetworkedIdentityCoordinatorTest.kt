package com.stripe.android.identity.networked

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.APIException
import com.stripe.android.identity.networking.models.NetworkedIdentityRoute
import org.junit.Test

@Suppress("LargeClass")
internal class NetworkedIdentityCoordinatorTest {

    @Test
    fun `start without a merchant publishable key falls back`() = runNetworkedIdentityScenario(
        config = niConfig(merchantPublishableKey = null)
    ) {
        coordinator.startReuse()

        assertFallback()
    }

    @Test
    fun `Link configuration failure falls back`() = runNetworkedIdentityScenario {
        coordinator.startReuse()
        runCurrent()
        val configure = linkSession.configureCalls.awaitItem()
        assertThat(configure.merchantPublishableKey).isEqualTo("pk_test_merchant")

        configure.response.complete(Result.failure(IllegalStateException("Link is not available")))

        assertFallback()
    }

    @Test
    fun `Link's own screens sign the user in with the known email and phone`() = runNetworkedIdentityScenario(
        config = niConfig(merchantEmail = "merchant@example.com", phoneNumberAutofillHint = "+15555551234")
    ) {
        startAndConfigure()

        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.LinkAuthentication)
        assertThat(coordinator.state.value.presentsIdentitySheet).isFalse()
        val linkUI = linkSession.linkUICalls.awaitItem()
        assertThat(linkUI.email).isEqualTo("merchant@example.com")
        assertThat(linkUI.phoneNumber).isEqualTo("+15555551234")
        assertThat(linkUI.mode).isEqualTo(NetworkedIdentityMode.Reuse)

        linkUI.response.complete(Result.success(niAccount(isVerified = true)))
        runCurrent()

        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.DocumentsPending)
        assertThat(repository.documentCalls.awaitItem().credentials).isEqualTo(
            NetworkedIdentityCredentials(publishableKey = "pk_consumer", sessionClientSecret = "session_secret")
        )
    }

    @Test
    fun `a failed Link sign-in falls back`() = runNetworkedIdentityScenario {
        startAndConfigure()

        linkSession.linkUICalls.awaitItem().response.complete(Result.failure(IllegalStateException("Attestation")))

        assertFallback()
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
    fun `unverified handed-in session signs in with Link's screens using its email`() = runNetworkedIdentityScenario(
        handoff = niHandoff()
    ) {
        startAndConfigure()
        linkSession.restoreCalls.awaitItem().response.complete(Result.success(niAccount(isVerified = false)))
        runCurrent()

        assertThat(linkSession.linkUICalls.awaitItem().email).isEqualTo("person@example.com")
    }

    @Test
    fun `expired handed-in session signs in with Link's screens using its email`() = runNetworkedIdentityScenario(
        handoff = niHandoff()
    ) {
        startAndConfigure()
        linkSession.restoreCalls.awaitItem().response.complete(Result.failure(IllegalStateException("expired")))
        runCurrent()

        assertThat(linkSession.linkUICalls.awaitItem().email).isEqualTo("person@example.com")
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
        assertThat(token.verificationSessionId).isEqualTo("vs_target")
        token.response.complete(Result.success(NetworkedIdentityAssociationToken("token_1")))
        runCurrent()
        val attach = actions.attachCalls.awaitItem()
        assertThat(attach.associationToken).isEqualTo("token_1")
        attach.response.complete(Result.success(niActionPageData()))
        assertThat(verificationUpdates.awaitItem()).isEqualTo(niActionPageData())
        runCurrent()

        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.DocumentShared(niDocument()))
        outcomes.expectNoEvents()

        coordinator.continueAfterSuccess()

        assertThat(outcomes.awaitItem())
            .isEqualTo(NetworkedIdentityOutcome.DocumentShared)
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

        assertFallback()
    }

    @Test
    fun `a rejected attach is retried once with a fresh token`() = runNetworkedIdentityScenario {
        reachDocuments()
        shareWithToken("token_1")

        actions.attachCalls.awaitItem().response.complete(Result.failure(apiException(statusCode = 400)))
        runCurrent()
        actions.committedChecks.awaitItem().also {
            assertThat(it.route).isEqualTo(NetworkedIdentityRoute.ResumeReuse)
        }.response.complete(Result.success(null))
        runCurrent()
        repository.tokenCalls.awaitItem().response.complete(
            Result.success(NetworkedIdentityAssociationToken("token_2"))
        )
        runCurrent()
        val retry = actions.attachCalls.awaitItem()
        assertThat(retry.associationToken).isEqualTo("token_2")
        retry.response.complete(Result.success(niActionPageData()))
        assertThat(verificationUpdates.awaitItem()).isEqualTo(niActionPageData())
        runCurrent()

        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.DocumentShared(niDocument()))
    }

    @Test
    fun `a lost attach response continues when the session shows the attachment`() = runNetworkedIdentityScenario {
        reachDocuments()
        shareWithToken("token_1")

        actions.attachCalls.awaitItem().response.complete(Result.failure(APIConnectionException("Lost.")))
        runCurrent()
        actions.committedChecks.awaitItem().response.complete(Result.success(niActionPageData()))
        assertThat(verificationUpdates.awaitItem()).isEqualTo(niActionPageData())
        runCurrent()

        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.DocumentShared(niDocument()))
        repository.tokenCalls.expectNoEvents()
    }

    @Test
    fun `an attach whose session can't be read back falls back without another token`() =
        runNetworkedIdentityScenario {
            reachDocuments()
            shareWithToken("token_1")

            actions.attachCalls.awaitItem().response.complete(Result.failure(APIConnectionException("Lost.")))
            runCurrent()
            actions.committedChecks.awaitItem().response.complete(Result.failure(APIConnectionException("Lost.")))

            assertFallback()
            repository.tokenCalls.expectNoEvents()
        }

    @Test
    fun `an attach failing twice falls back`() = runNetworkedIdentityScenario {
        reachDocuments()
        shareWithToken("token_1")

        actions.attachCalls.awaitItem().response.complete(Result.failure(apiException(statusCode = 429)))
        runCurrent()
        actions.committedChecks.awaitItem().response.complete(Result.success(null))
        runCurrent()
        repository.tokenCalls.awaitItem().response.complete(
            Result.success(NetworkedIdentityAssociationToken("token_2"))
        )
        runCurrent()
        actions.attachCalls.awaitItem().response.complete(Result.failure(APIConnectionException("Lost.")))
        runCurrent()
        actions.committedChecks.awaitItem().response.complete(Result.success(null))

        assertFallback()
    }

    @Test
    fun `unavailable Networked Identity falls back without retrying`() = runNetworkedIdentityScenario {
        reachDocuments()
        shareWithToken("token_1")

        actions.attachCalls.awaitItem().response.complete(
            Result.failure(apiException(statusCode = 400, code = "networked_identity_unavailable"))
        )

        assertFallback()
        actions.committedChecks.expectNoEvents()
        repository.tokenCalls.expectNoEvents()
    }

    @Test
    fun `a mismatched session falls back without retrying`() = runNetworkedIdentityScenario {
        reachDocuments()
        shareWithToken("token_1")

        actions.attachCalls.awaitItem().response.complete(Result.failure(apiException(statusCode = 404)))

        assertFallback()
        actions.committedChecks.expectNoEvents()
    }

    @Test
    fun `a lost save response continues when the session shows the save`() = runNetworkedIdentityScenario(
        config = niConfig(route = NetworkedIdentityRoute.Save)
    ) {
        startAndConfigure(NetworkedIdentityMode.Save)
        signInWithLink()
        repository.saveTokenCalls.awaitItem().response.complete(
            Result.success(NetworkedIdentityAssociationToken("save_token"))
        )
        runCurrent()

        actions.prepareSaveCalls.awaitItem().response.complete(Result.failure(apiException(statusCode = 502)))
        runCurrent()
        actions.committedChecks.awaitItem().also {
            assertThat(it.route).isEqualTo(NetworkedIdentityRoute.ResumeSave)
        }.response.complete(Result.success(niActionPageData()))
        assertThat(verificationUpdates.awaitItem()).isEqualTo(niActionPageData())
        runCurrent()

        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.SavePrepared)
        assertThat(coordinator.entry.value.offersSave).isFalse()
        repository.saveTokenCalls.expectNoEvents()
    }

    @Test
    fun `no eligible documents falls back to capture`() = runNetworkedIdentityScenario {
        reachDocuments(listOf(niDocument().copy(liveCaptured = false)))

        assertFallback()
    }

    @Test
    fun `save signs in with Link's screens then prepares the save`() = runNetworkedIdentityScenario(
        config = niConfig(route = NetworkedIdentityRoute.Save, merchantEmail = "person@example.com")
    ) {
        startAndConfigure(NetworkedIdentityMode.Save)
        val linkUI = linkSession.linkUICalls.awaitItem()
        assertThat(linkUI.email).isEqualTo("person@example.com")
        assertThat(linkUI.mode).isEqualTo(NetworkedIdentityMode.Save)
        linkUI.response.complete(Result.success(niAccount(isVerified = true)))
        runCurrent()

        val saveToken = repository.saveTokenCalls.awaitItem()
        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.SavePending)
        assertThat(saveToken.credentials.sessionClientSecret).isEqualTo("session_secret")
        assertThat(saveToken.verificationSessionId).isEqualTo("vs_target")
        saveToken.response.complete(Result.success(NetworkedIdentityAssociationToken("save_token")))
        runCurrent()
        val prepare = actions.prepareSaveCalls.awaitItem()
        assertThat(prepare.associationToken).isEqualTo("save_token")
        prepare.response.complete(Result.success(niActionPageData()))
        assertThat(verificationUpdates.awaitItem()).isEqualTo(niActionPageData())
        runCurrent()

        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.SavePrepared)
        assertThat(coordinator.entry.value.offersSave).isFalse()
        outcomes.expectNoEvents()

        coordinator.continueAfterSuccess()

        assertThat(outcomes.awaitItem()).isEqualTo(NetworkedIdentityOutcome.SavePrepared)
    }

    @Test
    fun `a failed save keeps the sheet open until it is closed`() = runNetworkedIdentityScenario(
        config = niConfig(route = NetworkedIdentityRoute.Save)
    ) {
        startAndConfigure(NetworkedIdentityMode.Save)
        signInWithLink()

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
        linkSession.linkUICalls.awaitItem()

        coordinator.cancel()
        runCurrent()
        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.Cancelled)
        assertThat(outcomes.awaitItem()).isEqualTo(NetworkedIdentityOutcome.Cancelled)

        coordinator.startReuse()
        runCurrent()

        // Link stays configured, and the Link session is never logged out.
        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.LinkAuthentication)
        linkSession.linkUICalls.awaitItem()
    }

    @Test
    fun `responses from a cancelled attempt are ignored`() = runNetworkedIdentityScenario {
        startAndConfigure()
        val linkUI = linkSession.linkUICalls.awaitItem()
        coordinator.cancel()
        runCurrent()
        outcomes.awaitItem()

        linkUI.response.complete(Result.success(niAccount(isVerified = true)))
        runCurrent()

        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.Cancelled)
    }

    @Test
    fun `manual capture records the skip and continues with manual capture`() = runNetworkedIdentityScenario {
        startAndConfigure()
        linkSession.linkUICalls.awaitItem()

        coordinator.chooseManualCapture()
        runCurrent()
        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.SkipPending)
        actions.skipCalls.awaitItem().response.complete(Result.success(niActionPageData()))
        assertThat(verificationUpdates.awaitItem()).isEqualTo(niActionPageData())
        runCurrent()

        assertThat(outcomes.awaitItem()).isEqualTo(NetworkedIdentityOutcome.ManualCapture)
        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.Idle)
        assertThat(coordinator.entry.value.reuseAvailable).isFalse()
    }

    @Test
    fun `failed skip still continues with ordinary verification`() = runNetworkedIdentityScenario {
        coordinator.chooseManualCapture()
        runCurrent()
        actions.skipCalls.awaitItem().response.complete(Result.failure(IllegalStateException("Skip failed.")))
        runCurrent()

        // The choice isn't recorded, but the user isn't blocked from verifying.
        assertThat(outcomes.awaitItem())
            .isEqualTo(NetworkedIdentityOutcome.Fallback)
        assertThat(coordinator.entry.value.reuseAvailable).isTrue()
    }

    @Test
    fun `manual capture waits for an issued attachment before skipping`() = runNetworkedIdentityScenario {
        reachDocuments()
        coordinator.shareSelectedDocument()
        runCurrent()
        repository.tokenCalls.awaitItem().response.complete(
            Result.success(NetworkedIdentityAssociationToken("token_1"))
        )
        runCurrent()
        val attach = actions.attachCalls.awaitItem()

        coordinator.chooseManualCapture()
        runCurrent()
        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.SkipPending)
        actions.skipCalls.expectNoEvents()
        outcomes.expectNoEvents()

        attach.response.complete(Result.success(niActionPageData()))
        assertThat(verificationUpdates.awaitItem()).isEqualTo(niActionPageData())
        runCurrent()
        actions.skipCalls.awaitItem().response.complete(Result.success(niActionPageData()))
        assertThat(verificationUpdates.awaitItem()).isEqualTo(niActionPageData())
        runCurrent()

        assertThat(outcomes.awaitItem()).isEqualTo(NetworkedIdentityOutcome.ManualCapture)
        assertThat(coordinator.entry.value.offersSave).isFalse()
    }

    @Test
    fun `closing during an issued attachment still updates the host requirements`() =
        runNetworkedIdentityScenario {
            reachDocuments()
            coordinator.shareSelectedDocument()
            runCurrent()
            repository.tokenCalls.awaitItem().response.complete(
                Result.success(NetworkedIdentityAssociationToken("token_1"))
            )
            runCurrent()
            val attach = actions.attachCalls.awaitItem()

            coordinator.cancel()
            attach.response.complete(Result.success(niActionPageData()))
            runCurrent()

            assertThat(verificationUpdates.awaitItem()).isEqualTo(niActionPageData())
            assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.Cancelled)
            assertThat(outcomes.awaitItem()).isEqualTo(NetworkedIdentityOutcome.Cancelled)
            // An attached ID is already in Link, so saving isn't offered.
            assertThat(coordinator.entry.value.offersSave).isFalse()
        }

    @Test
    fun `an attachment the host rejects isn't shared`() = runNetworkedIdentityScenario(hostAcceptsUpdates = false) {
        reachDocuments()
        shareWithToken("token_1")
        actions.attachCalls.awaitItem().response.complete(Result.success(niActionPageData()))
        assertThat(verificationUpdates.awaitItem()).isEqualTo(niActionPageData())

        assertFallback()
        assertThat(coordinator.entry.value.reuseAvailable).isTrue()
    }

    @Test
    fun `reopening Link waits for the previous write before attaching again`() = runNetworkedIdentityScenario {
        reachDocuments()
        coordinator.shareSelectedDocument()
        runCurrent()
        repository.tokenCalls.awaitItem().response.complete(
            Result.success(NetworkedIdentityAssociationToken("token_1"))
        )
        runCurrent()
        val firstAttach = actions.attachCalls.awaitItem()
        coordinator.cancel()
        runCurrent()
        assertThat(outcomes.awaitItem()).isEqualTo(NetworkedIdentityOutcome.Cancelled)

        coordinator.startReuse()
        runCurrent()
        repository.documentCalls.awaitItem().response.complete(Result.success(listOf(niDocument("document_2"))))
        runCurrent()
        coordinator.shareSelectedDocument()
        runCurrent()
        repository.tokenCalls.awaitItem().response.complete(
            Result.success(NetworkedIdentityAssociationToken("token_2"))
        )
        runCurrent()
        actions.attachCalls.expectNoEvents()

        firstAttach.response.complete(Result.success(niActionPageData()))
        assertThat(verificationUpdates.awaitItem()).isEqualTo(niActionPageData())
        runCurrent()
        val secondAttach = actions.attachCalls.awaitItem()
        assertThat(secondAttach.associationToken).isEqualTo("token_2")
        secondAttach.response.complete(Result.success(niActionPageData()))
        assertThat(verificationUpdates.awaitItem()).isEqualTo(niActionPageData())
        runCurrent()

        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.DocumentShared(niDocument("document_2")))
    }

    @Test
    fun `save after no reusable documents uses the same authenticated account`() = runNetworkedIdentityScenario {
        reachDocuments(emptyList())
        assertFallback()

        coordinator.startSave()
        runCurrent()

        // No second configure, lookup or code: the signed-in account saves right away.
        repository.saveTokenCalls.awaitItem()
        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.SavePending)
    }

    @Test
    fun `dismissing Link's screens cancels`() = runNetworkedIdentityScenario(
        config = niConfig(route = NetworkedIdentityRoute.Save)
    ) {
        startAndConfigure(NetworkedIdentityMode.Save)
        val linkUI = linkSession.linkUICalls.awaitItem()
        // Without a known email, Link's screens ask for it.
        assertThat(linkUI.email).isNull()

        linkUI.response.complete(Result.success(null))
        runCurrent()

        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.Cancelled)
        assertThat(outcomes.awaitItem()).isEqualTo(NetworkedIdentityOutcome.Cancelled)
    }

    @Test
    fun `abandon stops without reporting an outcome`() = runNetworkedIdentityScenario {
        startAndConfigure()
        linkSession.linkUICalls.awaitItem()

        coordinator.abandon()
        runCurrent()

        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.Cancelled)
        outcomes.expectNoEvents()
    }

    private suspend fun NetworkedIdentityTestScenario.shareWithToken(token: String) {
        coordinator.shareSelectedDocument()
        runCurrent()
        repository.tokenCalls.awaitItem().response.complete(Result.success(NetworkedIdentityAssociationToken(token)))
        runCurrent()
    }

    private fun apiException(statusCode: Int, code: String? = null) = APIException(
        stripeError = StripeError(type = "invalid_request_error", code = code),
        statusCode = statusCode,
    )
}
