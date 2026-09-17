package com.stripe.android.identity.networked

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.identity.networking.models.NetworkedIdentityRoute
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
internal fun runNetworkedIdentityScenario(
    config: NetworkedIdentityConfig = niConfig(),
    handoff: IdentityVerificationSheet.Configuration.LinkSessionHandoff? = null,
    block: suspend NetworkedIdentityTestScenario.() -> Unit
) = runTest {
    val linkSession = FakeNetworkedIdentityLinkSession()
    val repository = FakeNetworkedIdentityRepository()
    val actions = FakeNetworkedIdentityActions()
    val dispatcher = StandardTestDispatcher(testScheduler)
    val coordinator = NetworkedIdentityCoordinator(
        linkSession = linkSession,
        repository = repository,
        actions = actions,
        documentRequirements = NetworkedIdentityDocumentRequirements(
            allowedDocumentTypes = setOf(NetworkedIdentityDocumentType.PASSPORT),
            requiresLiveCapture = true
        ),
        config = config,
        handoff = handoff,
        merchantDisplayName = "Merchant",
        currentTimeSeconds = { 100L },
        dispatcher = dispatcher,
    )
    val outcomes = Turbine<NetworkedIdentityOutcome>()
    backgroundScope.launch(dispatcher) { coordinator.outcomes.collect { outcomes.add(it) } }

    NetworkedIdentityTestScenario(coordinator, linkSession, repository, actions, outcomes, this).block()

    linkSession.ensureAllEventsConsumed()
    repository.ensureAllEventsConsumed()
    actions.ensureAllEventsConsumed()
    outcomes.ensureAllEventsConsumed()
}

internal fun niConfig(
    route: NetworkedIdentityRoute = NetworkedIdentityRoute.Reuse,
    merchantPublishableKey: String? = "pk_test_merchant",
    merchantEmail: String? = null,
) = NetworkedIdentityConfig(
    route = route,
    merchantPublishableKey = merchantPublishableKey,
    merchantEmail = merchantEmail,
    seedSavedDocuments = false,
)

internal fun niHandoff() = IdentityVerificationSheet.Configuration.LinkSessionHandoff(
    email = "person@example.com",
    consumerSessionClientSecret = "handoff_secret",
    consumerPublishableKey = "pk_consumer_handoff",
)

@OptIn(ExperimentalCoroutinesApi::class)
internal data class NetworkedIdentityTestScenario(
    val coordinator: NetworkedIdentityCoordinator,
    val linkSession: FakeNetworkedIdentityLinkSession,
    val repository: FakeNetworkedIdentityRepository,
    val actions: FakeNetworkedIdentityActions,
    val outcomes: Turbine<NetworkedIdentityOutcome>,
    val scope: TestScope,
) {
    fun runCurrent() = scope.runCurrent()

    /** Starts [mode] with a user tap and completes Link configuration. */
    suspend fun startAndConfigure(mode: NetworkedIdentityMode = NetworkedIdentityMode.Reuse) {
        when (mode) {
            NetworkedIdentityMode.Reuse -> coordinator.startReuse()
            NetworkedIdentityMode.Save -> coordinator.startSave()
        }
        runCurrent()
        linkSession.configureCalls.awaitItem().response.complete(Result.success(Unit))
        runCurrent()
    }

    /** From the email step: submits an email that belongs to [account], or to no account when null. */
    suspend fun signInWithEmail(account: NetworkedIdentityLinkAccount? = niAccount()) {
        coordinator.submitEmail("person@example.com")
        runCurrent()
        linkSession.lookupCalls.awaitItem().response.complete(Result.success(account))
        runCurrent()
    }

    /** While a code is being sent: completes sending. */
    suspend fun completeCodeSent() {
        linkSession.startVerificationCalls.awaitItem().response.complete(Result.success(niAccount()))
        runCurrent()
    }

    suspend fun confirmCode(verified: NetworkedIdentityLinkAccount = niAccount(isVerified = true)) {
        coordinator.submitOtp("123456")
        runCurrent()
        linkSession.confirmVerificationCalls.awaitItem().response.complete(Result.success(verified))
        runCurrent()
    }

    suspend fun reachDocuments(documents: List<NetworkedIdentityDocument> = listOf(niDocument())) {
        startAndConfigure()
        signInWithEmail()
        completeCodeSent()
        confirmCode()
        repository.documentCalls.awaitItem().response.complete(Result.success(documents))
        runCurrent()
    }

    suspend fun assertFallback(reason: NetworkedIdentityFallbackReason) {
        runCurrent()
        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.FullCaptureFallback(reason))
        if (reason == NetworkedIdentityFallbackReason.UserSelectedManualCapture) {
            actions.skipCalls.awaitItem().response.complete(Result.success(niActionPageData()))
        }
        assertThat(outcomes.awaitItem()).isEqualTo(NetworkedIdentityOutcome.Fallback(reason))
    }
}
