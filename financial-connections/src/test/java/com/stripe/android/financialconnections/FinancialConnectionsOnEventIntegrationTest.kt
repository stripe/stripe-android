package com.stripe.android.financialconnections

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeResponse
import com.stripe.android.financialconnections.FinancialConnectionsSheetViewEffect.FinishWithResult
import com.stripe.android.financialconnections.FinancialConnectionsSheetViewEffect.OpenNativeAuthFlow
import com.stripe.android.financialconnections.analytics.FakeFinancialConnectionsAnalyticsEventSender
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTrackerImpl
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Name
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEventContext
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEventEmitter
import com.stripe.android.financialconnections.analytics.FinancialConnectionsResponseEventEmitter
import com.stripe.android.financialconnections.browser.BrowserManager
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.NativeAuthFlowRouter
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs.ForData
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Canceled
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Failed
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.SynchronizeSessionResponse
import com.stripe.android.financialconnections.network.FakeEventNetworkClient
import com.stripe.android.financialconnections.network.FinancialConnectionsRequestExecutor
import com.stripe.android.financialconnections.repository.FinancialConnectionsManifestRepository
import com.stripe.android.financialconnections.utils.TestIntegrityRequestManager
import com.stripe.android.testing.ViewModelStoreTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
internal class FinancialConnectionsOnEventIntegrationTest {

    private val dispatcher = StandardTestDispatcher()

    @get:Rule
    val coroutineRule = CoroutineTestRule(dispatcher)

    @get:Rule
    val viewModelStoreRule = ViewModelStoreTestRule()

    private val sync = ApiKeyFixtures.syncResponse().let {
        it.copy(manifest = it.manifest.copy(id = "fcsess_from_synchronize"))
    }
    private val configuration = FinancialConnectionsSheetConfiguration(
        financialConnectionsSessionClientSecret = ApiKeyFixtures.DEFAULT_FINANCIAL_CONNECTIONS_SESSION_SECRET,
        publishableKey = ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY,
        preCollectedConsent = null
    )

    @Test
    fun `browser open and completion share the synchronized ID without another request`() = runScenario {
        assertThat(networkClient.requests.awaitItem().url).endsWith("/synchronize")
        awaitEvent(Name.OPEN)
        awaitEvent(Name.FLOW_LAUNCHED_IN_BROWSER)

        viewModel.onDismissed()

        awaitEvent(Name.CANCEL)
        assertThat((viewModel.stateFlow.value.viewEffect as FinishWithResult).result).isEqualTo(Canceled)
        networkClient.requests.expectNoEvents()
    }

    @Test
    fun `first native event uses the same canonical ID passed to the native activity`() = runScenario(native = true) {
        assertThat(networkClient.requests.awaitItem().url).endsWith("/synchronize")
        val opened = awaitEvent(Name.OPEN)
        val effect = viewModel.stateFlow.value.viewEffect as OpenNativeAuthFlow

        assertThat(effect.initialSyncResponse.manifest.id).isEqualTo(opened.financialConnectionsSessionId)
        publicEvents.expectNoEvents()
        networkClient.requests.expectNoEvents()
    }

    @Test
    fun `synchronize failure completes without a public event or retry for an ID`() = runScenario(
        response = StripeResponse(
            code = 400,
            body = """
                {"error":{"message":"Session expired","extra_fields":{
                    "events_to_emit":[{"type":"error","error":{"error_code":"session_expired"}}]
                }}}
            """.trimIndent()
        )
    ) {
        assertThat(networkClient.requests.awaitItem().url).endsWith("/synchronize")
        val result = (viewModel.stateFlow.value.viewEffect as FinishWithResult).result as Failed

        assertThat(result.error).isInstanceOf(InvalidRequestException::class.java)
        publicEvents.expectNoEvents()
        analyticsSender.calls.expectNoEvents()
        networkClient.requests.expectNoEvents()
    }

    @Test
    fun `restored browser presentation retains its canonical ID without synchronization`() = runScenario(
        restoredManifest = sync.manifest
    ) {
        viewModel.onDismissed()

        awaitEvent(Name.CANCEL)
        assertThat((viewModel.stateFlow.value.viewEffect as FinishWithResult).result).isEqualTo(Canceled)
        networkClient.requests.expectNoEvents()
    }

    @Test
    fun `empty canonical ID fails initialization without a public event`() = runScenario(
        response = StripeResponse(
            code = 200,
            body = Json.encodeToString(
                SynchronizeSessionResponse.serializer(),
                sync.copy(manifest = sync.manifest.copy(id = ""))
            )
        )
    ) {
        assertThat(networkClient.requests.awaitItem().url).endsWith("/synchronize")
        val result = (viewModel.stateFlow.value.viewEffect as FinishWithResult).result as Failed

        assertThat(result.error).isInstanceOf(IllegalArgumentException::class.java)
        publicEvents.expectNoEvents()
        analyticsSender.calls.expectNoEvents()
        networkClient.requests.expectNoEvents()
    }

    @Test
    fun `missing canonical ID fails initialization without a public event`() = runScenario(
        response = StripeResponse(
            code = 200,
            body = Json.parseToJsonElement(Json.encodeToString(SynchronizeSessionResponse.serializer(), sync))
                .jsonObject.let { response ->
                    JsonObject(
                        response + ("manifest" to JsonObject(response.getValue("manifest").jsonObject - "id"))
                    ).toString()
                }
        )
    ) {
        assertThat(networkClient.requests.awaitItem().url).endsWith("/synchronize")
        val result = (viewModel.stateFlow.value.viewEffect as FinishWithResult).result as Failed

        assertThat(result.error).isInstanceOf(SerializationException::class.java)
        publicEvents.expectNoEvents()
        analyticsSender.calls.expectNoEvents()
        networkClient.requests.expectNoEvents()
    }

    private fun runScenario(
        response: StripeResponse<String> = StripeResponse(
            code = 200,
            body = Json.encodeToString(SynchronizeSessionResponse.serializer(), sync)
        ),
        native: Boolean = false,
        restoredManifest: FinancialConnectionsSessionManifest? = null,
        block: suspend Scenario.() -> Unit
    ) = runTest(dispatcher) {
        val publicEvents = Turbine<FinancialConnectionsEvent>()
        val analyticsSender = FakeFinancialConnectionsAnalyticsEventSender()
        val eventContext = FinancialConnectionsEventContext(restoredManifest)
        val eventEmitter = FinancialConnectionsEventEmitter(
            eventContext = eventContext,
            analyticsSender = analyticsSender,
            logger = Logger.noop(),
            workContext = backgroundScope.coroutineContext
        )
        val networkClient = FakeEventNetworkClient(response)
        val requestExecutor = FinancialConnectionsRequestExecutor(
            stripeNetworkClient = networkClient,
            eventEmitter = FinancialConnectionsResponseEventEmitter(Json.Default, Logger.noop(), eventEmitter),
            json = Json.Default,
            logger = Logger.noop()
        )
        val repository = createRepository(requestExecutor, eventContext)
        val getOrFetchSync = GetOrFetchSync(repository, configuration, "com.example.app", mock())
        val browserManager = mock<BrowserManager>()
        whenever(browserManager.canOpenHttpsUrl()).thenReturn(true)
        val nativeRouter = mock<NativeAuthFlowRouter>()
        whenever(nativeRouter.nativeAuthFlowEnabled(any())).thenReturn(native)
        FinancialConnections.setEventListener(publicEvents::add)
        try {
            val viewModel = FinancialConnectionsSheetViewModel(
                applicationId = "com.example.app",
                savedStateHandle = SavedStateHandle(),
                getOrFetchSync = getOrFetchSync,
                integrityRequestManager = TestIntegrityRequestManager(prepareResult = Result.success(Unit)),
                integrityVerdictManager = mock(),
                fetchFinancialConnectionsSession = mock(),
                fetchFinancialConnectionsSessionForToken = mock(),
                logger = Logger.noop(),
                browserManager = browserManager,
                eventReporter = mock(),
                analyticsTracker = FinancialConnectionsAnalyticsTrackerImpl(
                    getOrFetchSync = getOrFetchSync,
                    analyticsSender = analyticsSender,
                    eventEmitter = eventEmitter
                ),
                eventContext = eventContext,
                nativeRouter = nativeRouter,
                nativeAuthFlowCoordinator = NativeAuthFlowCoordinator(),
                initialState = FinancialConnectionsSheetState(ForData(configuration), null).copy(
                    manifest = restoredManifest
                ),
                ioDispatcher = dispatcher
            ).also(viewModelStoreRule::track)
            testScheduler.runCurrent()
            Scenario(viewModel, networkClient, analyticsSender, publicEvents).block()
            testScheduler.runCurrent()
        } finally {
            FinancialConnections.clearEventListener()
            networkClient.requests.ensureAllEventsConsumed()
            analyticsSender.calls.ensureAllEventsConsumed()
            publicEvents.ensureAllEventsConsumed()
        }
    }

    private fun createRepository(
        requestExecutor: FinancialConnectionsRequestExecutor,
        eventContext: FinancialConnectionsEventContext
    ) = FinancialConnectionsManifestRepository(
        requestExecutor = requestExecutor,
        apiRequestFactory = ApiRequest.Factory(),
        provideApiRequestOptions = { ApiRequest.Options(apiKey = ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY) },
        logger = Logger.noop(),
        locale = Locale.US,
        initialSync = null,
        eventContext = eventContext
    )

    private data class Scenario(
        val viewModel: FinancialConnectionsSheetViewModel,
        val networkClient: FakeEventNetworkClient,
        val analyticsSender: FakeFinancialConnectionsAnalyticsEventSender,
        val publicEvents: Turbine<FinancialConnectionsEvent>
    ) {
        suspend fun awaitEvent(name: Name): FinancialConnectionsEvent {
            val event = publicEvents.awaitItem()
            assertThat(event.name).isEqualTo(name)
            assertThat(event.financialConnectionsSessionId).isEqualTo("fcsess_from_synchronize")
            val analytics = analyticsSender.calls.awaitItem()
            assertThat(analytics.event.eventName).isEqualTo("linked_accounts.external_on_event.emitted")
            assertThat(analytics.manifest.id).isEqualTo(event.financialConnectionsSessionId)
            return event
        }
    }
}
