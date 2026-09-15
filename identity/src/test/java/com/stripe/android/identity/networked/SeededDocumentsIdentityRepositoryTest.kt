package com.stripe.android.identity.networked

import com.google.common.truth.Truth.assertThat
import com.stripe.android.identity.SUCCESS_VERIFICATION_PAGE_REQUIRE_LIVE_CAPTURE
import com.stripe.android.identity.networking.models.ClearDataParam
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.Requirement
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageData
import com.stripe.android.identity.networking.models.VerificationPageDataRequirements
import com.stripe.android.identity.networking.models.VerificationPageRequirements
import com.stripe.android.identity.viewmodel.FakeIdentityHostRepository
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SeededDocumentsIdentityRepositoryTest {

    @Test
    fun `attaching a seeded token satisfies document capture without calling the backend`() = runScenario {
        val attached = actions.attachDocument(SEEDED_TOKEN).getOrThrow()

        assertThat(attached.id).isEqualTo(SESSION_ID)
        assertThat(attached.requirements.missings)
            .containsExactly(Requirement.BIOMETRICCONSENT, Requirement.FACE)
        delegateActions.attachCalls.expectNoEvents()
    }

    @Test
    fun `a seeded document completes after consent without recapture or a backend submit`() = runScenario(
        missing = listOf(Requirement.BIOMETRICCONSENT, Requirement.IDDOCUMENTFRONT, Requirement.IDDOCUMENTBACK),
    ) {
        actions.attachDocument(SEEDED_TOKEN).getOrThrow()
        val consent = async { postConsent() }
        runCurrent()
        delegate.dataCalls.awaitItem()
        delegate.dataResponse.complete(data(listOf(Requirement.IDDOCUMENTFRONT, Requirement.IDDOCUMENTBACK)))
        assertThat(consent.await().requirements.missings).isEmpty()

        val submitted = repository.postVerificationPageSubmit(SESSION_ID, EPHEMERAL_KEY)

        assertThat(submitted.submitted).isTrue()
        assertThat(submitted.closed).isTrue()
        assertThat(submitted.status).isEqualTo(VerificationPageData.Status.PROCESSING)
        delegate.submitCalls.expectNoEvents()
    }

    @Test
    fun `seeded reuse keeps the selfie and skip restores document capture`() = runScenario {
        actions.attachDocument(SEEDED_TOKEN).getOrThrow()
        val response = data(listOf(Requirement.IDDOCUMENTFRONT, Requirement.IDDOCUMENTBACK, Requirement.FACE))
        val consent = async { postConsent() }
        runCurrent()
        delegate.dataCalls.awaitItem()
        delegate.dataResponse.complete(response)
        assertThat(consent.await().requirements.missings).containsExactly(Requirement.FACE)

        // The selfie is still missing, so nothing is submitted yet.
        val premature = repository.postVerificationPageSubmit(SESSION_ID, EPHEMERAL_KEY)
        assertThat(premature.submitted).isFalse()
        assertThat(premature.requirements.missings).containsExactly(Requirement.FACE)

        assertThat(actions.skip().getOrThrow()).isEqualTo(response)
        delegate.submitCalls.expectNoEvents()
        delegateActions.skipCalls.expectNoEvents()
    }

    @Test
    fun `sample attachments are rejected for live sessions`() = runScenario(livemode = true) {
        assertThat(actions.attachDocument(SEEDED_TOKEN).isFailure).isTrue()
        delegateActions.attachCalls.expectNoEvents()
    }

    @Test
    fun `a sample save succeeds without calling the save endpoints, in any mode`() = runScenario(
        livemode = true,
        missing = emptyList(),
    ) {
        val prepared = actions.prepareDocumentSave(SeededDocumentsNetworkedIdentityRepository.SEEDED_SAVE_TOKEN)

        assertThat(prepared.getOrThrow().id).isEqualTo(SESSION_ID)
        delegateActions.prepareSaveCalls.expectNoEvents()
    }

    @Test
    fun `a real token goes to the backend`() = runScenario {
        val attach = async { actions.attachDocument("token_1") }
        runCurrent()
        val call = delegateActions.attachCalls.awaitItem()
        assertThat(call.associationToken).isEqualTo("token_1")
        call.response.complete(Result.success(niActionPageData()))

        assertThat(attach.await().getOrThrow()).isEqualTo(niActionPageData())
    }

    private suspend fun Scenario.postConsent() = repository.postVerificationPageData(
        id = SESSION_ID,
        ephemeralKey = EPHEMERAL_KEY,
        collectedDataParam = CollectedDataParam(biometricConsent = true),
        clearDataParam = ClearDataParam(),
    )

    private fun runScenario(
        livemode: Boolean = false,
        missing: List<Requirement> = listOf(
            Requirement.BIOMETRICCONSENT, Requirement.IDDOCUMENTFRONT, Requirement.IDDOCUMENTBACK, Requirement.FACE,
        ),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val page = SUCCESS_VERIFICATION_PAGE_REQUIRE_LIVE_CAPTURE.copy(
            id = SESSION_ID,
            livemode = livemode,
            status = VerificationPage.Status.REQUIRESINPUT,
            submitted = false,
            requirements = VerificationPageRequirements(missing),
        )
        val delegate = FakeIdentityHostRepository(page)
        val delegateActions = FakeNetworkedIdentityActions()
        val repository = SeededDocumentsIdentityRepository(delegate)
        repository.retrieveVerificationPage(SESSION_ID, EPHEMERAL_KEY)
        Scenario(repository, repository.actions(delegateActions), delegate, delegateActions, this).block()
        delegate.ensureAllEventsConsumed()
        delegateActions.ensureAllEventsConsumed()
    }

    private data class Scenario(
        val repository: SeededDocumentsIdentityRepository,
        val actions: NetworkedIdentityActions,
        val delegate: FakeIdentityHostRepository,
        val delegateActions: FakeNetworkedIdentityActions,
        val scope: TestScope,
    ) {
        fun runCurrent() = scope.runCurrent()

        fun <T> async(block: suspend () -> T): Deferred<T> = scope.async { block() }
    }

    private fun data(missing: List<Requirement>) = VerificationPageData(
        id = SESSION_ID,
        objectType = "identity.verification_page_data",
        requirements = VerificationPageDataRequirements(errors = emptyList(), missings = missing),
        status = VerificationPageData.Status.REQUIRESINPUT,
        submitted = false,
        closed = false,
    )

    private companion object {
        const val SESSION_ID = "vs_target"
        const val EPHEMERAL_KEY = "ek_test"
        const val SEEDED_TOKEN = "seeded_token_seeded_iddoc_passport"
    }
}
