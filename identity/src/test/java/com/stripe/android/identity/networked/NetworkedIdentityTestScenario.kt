package com.stripe.android.identity.networked

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
internal fun runNetworkedIdentityScenario(
    authSessionSecrets: List<String> = emptyList(),
    block: suspend NetworkedIdentityTestScenario.() -> Unit
) = runTest {
    val repository = FakeNetworkedIdentityRepository()
    val cancellations = Turbine<Unit>()
    val fallbacks = Turbine<NetworkedIdentityFallbackReason>()
    val coordinator = NetworkedIdentityCoordinator(
        repository = repository,
        documentRequirements = NetworkedIdentityDocumentRequirements(
            allowedDocumentTypes = setOf(NetworkedIdentityDocumentType.PASSPORT),
            requiresLiveCapture = true
        ),
        locale = "en-US",
        initialAuthSessionSecrets = authSessionSecrets,
        currentTimeSeconds = { 100L },
        dispatcher = StandardTestDispatcher(testScheduler),
        onCancel = { cancellations.add(Unit) },
        onFallback = { fallbacks.add(it) }
    )
    NetworkedIdentityTestScenario(coordinator, repository, this, cancellations, fallbacks).block()
    repository.ensureAllEventsConsumed()
    cancellations.ensureAllEventsConsumed()
    fallbacks.ensureAllEventsConsumed()
}

@OptIn(ExperimentalCoroutinesApi::class)
internal data class NetworkedIdentityTestScenario(
    val coordinator: NetworkedIdentityCoordinator,
    val repository: FakeNetworkedIdentityRepository,
    val scope: TestScope,
    val cancellations: Turbine<Unit>,
    val fallbacks: Turbine<NetworkedIdentityFallbackReason>
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

    suspend fun assertFallback(reason: NetworkedIdentityFallbackReason, expectLogout: Boolean = true) {
        runCurrent()
        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.FullCaptureFallback(reason))
        assertThat(fallbacks.awaitItem()).isEqualTo(reason)
        if (expectLogout) repository.logoutCalls.awaitItem()
        cancellations.expectNoEvents()
    }
}
