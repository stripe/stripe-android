package com.stripe.android.identity.networked

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.identity.networking.models.NetworkedIdentityRoute
import com.stripe.android.identity.networking.models.VerificationPageData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
internal fun runNetworkedIdentityScenario(
    config: NetworkedIdentityConfig = niConfig(),
    handoff: IdentityVerificationSheet.Configuration.LinkSessionHandoff? = null,
    hostAcceptsUpdates: Boolean = true,
    block: suspend NetworkedIdentityTestScenario.() -> Unit
) = runTest {
    val linkSession = FakeNetworkedIdentityLinkSession()
    val repository = FakeNetworkedIdentityRepository()
    val actions = FakeNetworkedIdentityActions()
    val verificationUpdates = Turbine<VerificationPageData>()
    val dispatcher = StandardTestDispatcher(testScheduler)
    val coordinator = niCoordinator(linkSession, repository, actions, config, handoff, dispatcher) {
        verificationUpdates.add(it)
        hostAcceptsUpdates
    }
    val outcomes = Turbine<NetworkedIdentityOutcome>()
    backgroundScope.launch(dispatcher) { coordinator.outcomes.collect { outcomes.add(it) } }

    NetworkedIdentityTestScenario(
        coordinator, linkSession, repository, actions, outcomes, verificationUpdates, this
    ).block()

    linkSession.ensureAllEventsConsumed()
    repository.ensureAllEventsConsumed()
    actions.ensureAllEventsConsumed()
    outcomes.ensureAllEventsConsumed()
    verificationUpdates.ensureAllEventsConsumed()
}

@Suppress("LongParameterList")
internal fun niCoordinator(
    linkSession: FakeNetworkedIdentityLinkSession,
    repository: FakeNetworkedIdentityRepository,
    actions: FakeNetworkedIdentityActions,
    config: NetworkedIdentityConfig,
    handoff: IdentityVerificationSheet.Configuration.LinkSessionHandoff?,
    dispatcher: CoroutineDispatcher,
    onVerificationUpdate: (VerificationPageData) -> Boolean,
) = NetworkedIdentityCoordinator(
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
    onVerificationUpdate = onVerificationUpdate,
    dispatcher = dispatcher,
)

/** A ViewModel over fakes whose calls run immediately, for screen tests. */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun niViewModel(
    linkSession: FakeNetworkedIdentityLinkSession = FakeNetworkedIdentityLinkSession(),
    actions: FakeNetworkedIdentityActions = FakeNetworkedIdentityActions(),
    config: NetworkedIdentityConfig = niConfig(),
    handoff: IdentityVerificationSheet.Configuration.LinkSessionHandoff? = null,
    includesSelfie: Boolean = false,
) = NetworkedIdentityViewModel(
    niCoordinator(
        linkSession, FakeNetworkedIdentityRepository(), actions, config, handoff, UnconfinedTestDispatcher()
    ) { true },
    includesSelfie,
)

internal fun niConfig(
    route: NetworkedIdentityRoute = NetworkedIdentityRoute.Reuse,
    merchantPublishableKey: String? = "pk_test_merchant",
    merchantEmail: String? = null,
    saveEnabled: Boolean = true,
    savedDocumentAvailable: Boolean = false,
    phoneNumberAutofillHint: String? = null,
) = NetworkedIdentityConfig(
    route = route,
    saveEnabled = saveEnabled,
    savedDocumentAvailable = savedDocumentAvailable,
    merchantPublishableKey = merchantPublishableKey,
    merchantEmail = merchantEmail,
    phoneNumberAutofillHint = phoneNumberAutofillHint,
    seedSavedDocuments = false,
)

internal fun niHandoff() = IdentityVerificationSheet.Configuration.LinkSessionHandoff(
    email = "person@example.com",
    linkSessionKey = "lsk_handoff",
)

@OptIn(ExperimentalCoroutinesApi::class)
internal data class NetworkedIdentityTestScenario(
    val coordinator: NetworkedIdentityCoordinator,
    val linkSession: FakeNetworkedIdentityLinkSession,
    val repository: FakeNetworkedIdentityRepository,
    val actions: FakeNetworkedIdentityActions,
    val outcomes: Turbine<NetworkedIdentityOutcome>,
    val verificationUpdates: Turbine<VerificationPageData>,
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

    /** While Link's own screens are shown: the user signs in as [account]. */
    suspend fun signInWithLink(account: NetworkedIdentityLinkAccount = niAccount(isVerified = true)) {
        linkSession.linkUICalls.awaitItem().response.complete(Result.success(account))
        runCurrent()
    }

    suspend fun reachDocuments(documents: List<NetworkedIdentityDocument> = listOf(niDocument())) {
        startAndConfigure()
        signInWithLink()
        repository.documentCalls.awaitItem().response.complete(Result.success(documents))
        runCurrent()
    }

    suspend fun assertFallback() {
        runCurrent()
        assertThat(coordinator.state.value).isEqualTo(NetworkedIdentityState.FullCaptureFallback)
        assertThat(outcomes.awaitItem()).isEqualTo(NetworkedIdentityOutcome.Fallback)
    }
}
