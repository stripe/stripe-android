package com.stripe.android.identity.networked

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.identity.networking.models.VerificationPageData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
internal fun runNetworkedIdentityScenario(
    authSessionSecrets: List<String> = emptyList(),
    withActions: Boolean = false,
    currentTimeSeconds: () -> Long = { 100L },
    block: suspend NetworkedIdentityTestScenario.() -> Unit
) = runTest {
    val repository = FakeNetworkedIdentityRepository()
    val cancellations = Turbine<Unit>()
    val fallbacks = Turbine<NetworkedIdentityFallbackReason>()
    val completions = Turbine<Result<VerificationPageData>>()
    val actions = FakeNetworkedIdentityActions()
    val coordinator = NetworkedIdentityCoordinator(
        repository = repository,
        actions = actions.takeIf { withActions },
        documentRequirements = NetworkedIdentityDocumentRequirements(
            allowedDocumentTypes = setOf(NetworkedIdentityDocumentType.PASSPORT),
            requiresLiveCapture = true
        ),
        locale = "en-US",
        initialAuthSessionSecrets = authSessionSecrets,
        currentTimeSeconds = currentTimeSeconds,
        dispatcher = StandardTestDispatcher(testScheduler),
        onCancel = { cancellations.add(Unit) },
        onFallback = { fallbacks.add(it) },
        onComplete = { completions.add(it) }
    )
    NetworkedIdentityTestScenario(coordinator, repository, this, cancellations, fallbacks, actions, completions).block()
    repository.ensureAllEventsConsumed()
    cancellations.ensureAllEventsConsumed()
    fallbacks.ensureAllEventsConsumed()
    actions.ensureAllEventsConsumed()
    completions.ensureAllEventsConsumed()
}

@OptIn(ExperimentalCoroutinesApi::class)
internal data class NetworkedIdentityTestScenario(
    val coordinator: NetworkedIdentityCoordinator,
    val repository: FakeNetworkedIdentityRepository,
    val scope: TestScope,
    val cancellations: Turbine<Unit>,
    val fallbacks: Turbine<NetworkedIdentityFallbackReason>,
    val actions: FakeNetworkedIdentityActions,
    val completions: Turbine<Result<VerificationPageData>>
) {
    fun runCurrent() = scope.runCurrent()

    suspend fun startEmail(
        found: NetworkedIdentityLookup.Found = niFound()
    ): FakeNetworkedIdentityRepository.StartCall {
        coordinator.submitEmail("person@example.com")
        runCurrent()
        val lookup = repository.lookupCalls.awaitItem()
        lookup.response.complete(Result.success(found))
        runCurrent()
        return repository.startCalls.awaitItem()
    }

    suspend fun awaitOtp() {
        startEmail().response.complete(Result.success(niResponse()))
        runCurrent()
    }

    suspend fun confirmOtp(): FakeNetworkedIdentityRepository.ConfirmCall {
        coordinator.submitOtp("123456")
        runCurrent()
        return repository.confirmCalls.awaitItem()
    }

    suspend fun resendOtp(): FakeNetworkedIdentityRepository.StartCall {
        coordinator.resendOtp()
        runCurrent()
        return repository.startCalls.awaitItem()
    }

    suspend fun loadDocuments(): FakeNetworkedIdentityRepository.DocumentCall {
        awaitOtp()
        confirmOtp().response.complete(
            Result.success(
                niResponse(
                    clientSecret = "session_confirmed",
                    verificationSessions = listOf(niSms(state = NetworkedIdentityVerificationState.VERIFIED)),
                    authSessionClientSecret = "auth_confirmed"
                )
            )
        )
        runCurrent()
        return repository.documentCalls.awaitItem()
    }

    suspend fun selectDocument() {
        loadDocuments().response.complete(Result.success(listOf(niDocument("selected"))))
        runCurrent()
        coordinator.selectDocument("selected")
    }

    suspend fun requestAssociationToken(): FakeNetworkedIdentityRepository.AssociationTokenCall {
        coordinator.continueWithSelectedDocument()
        runCurrent()
        return repository.associationTokenCalls.awaitItem()
    }

    suspend fun beginAttachment(): FakeNetworkedIdentityActions.TokenCall {
        selectDocument()
        requestAssociationToken().response.complete(Result.success(NetworkedIdentityAssociationToken("reuse_token")))
        runCurrent()
        return actions.attachCalls.awaitItem()
    }

    suspend fun assertFallback(reason: NetworkedIdentityFallbackReason, expectLogout: Boolean = true) {
        runCurrent()
        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.FullCaptureFallback(reason))
        assertThat(fallbacks.awaitItem()).isEqualTo(reason)
        if (expectLogout) repository.logoutCalls.awaitItem()
        cancellations.expectNoEvents()
    }
}
