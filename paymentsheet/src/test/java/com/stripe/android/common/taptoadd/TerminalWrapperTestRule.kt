package com.stripe.android.common.taptoadd

import android.content.Context
import app.cash.turbine.Turbine
import com.stripe.android.model.PaymentMethod
import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.external.callable.Callback
import com.stripe.stripeterminal.external.callable.ConnectionTokenProvider
import com.stripe.stripeterminal.external.callable.DiscoveryListener
import com.stripe.stripeterminal.external.callable.ReaderCallback
import com.stripe.stripeterminal.external.callable.SetupIntentCallback
import com.stripe.stripeterminal.external.callable.TerminalListener
import com.stripe.stripeterminal.external.models.AllowRedisplay
import com.stripe.stripeterminal.external.models.CollectSetupIntentConfiguration
import com.stripe.stripeterminal.external.models.ConnectionConfiguration
import com.stripe.stripeterminal.external.models.DeviceType
import com.stripe.stripeterminal.external.models.DiscoveryConfiguration
import com.stripe.stripeterminal.external.models.Reader
import com.stripe.stripeterminal.external.models.ReaderSupportResult
import com.stripe.stripeterminal.external.models.SetupIntent
import com.stripe.stripeterminal.external.models.TapToPayUxConfiguration
import com.stripe.stripeterminal.external.models.TerminalException
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.mockito.kotlin.KStubbing
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class TerminalWrapperTestRule : TestWatcher() {
    private lateinit var scenario: Scenario

    private val isInitializedCalls = Turbine<Unit>()
    private val initTerminalCalls = Turbine<InitTerminalCall>()
    private val supportsReadersOfTypeCalls = Turbine<SupportsReadersOfTypeCall>()
    private val connectedReaderCalls = Turbine<Unit>()
    private val discoverReadersCalls = Turbine<DiscoverReadersCall>()
    private val connectReaderCalls = Turbine<ConnectReaderCall>()
    private val setTapToPayUxConfigurationCalls = Turbine<TapToPayUxConfiguration>()
    private val retrieveSetupIntentCalls = Turbine<RetrieveSetupIntentCall>()
    private val collectSetupIntentPaymentMethodCalls = Turbine<CollectSetupIntentPaymentMethodCall>()
    private val confirmSetupIntentCalls = Turbine<ConfirmSetupIntentCall>()

    override fun starting(description: Description?) {
        TerminalWrapper.overrideWrapper = FakeTerminalWrapper(
            terminal = createMockedTerminalInstance(),
            isInitializedCalls = isInitializedCalls,
            initTerminalCalls = initTerminalCalls,
            isInitialized = { scenario.isInitialized },
        )
        super.starting(description)
    }

    override fun finished(description: Description?) {
        TerminalWrapper.overrideWrapper = null
        validate()
        super.finished(description)
    }

    fun enqueueScenario(scenario: Scenario) {
        this.scenario = scenario
    }

    suspend fun awaitIsInitializedCall() = isInitializedCalls.awaitItem()

    suspend fun awaitInitTerminalCall() = initTerminalCalls.awaitItem()

    suspend fun awaitSupportsReadersOfTypeCall() = supportsReadersOfTypeCalls.awaitItem()

    suspend fun awaitConnectedReaderCall() = connectedReaderCalls.awaitItem()

    suspend fun awaitDiscoverReadersCall() = discoverReadersCalls.awaitItem()

    suspend fun awaitConnectReaderCall() = connectReaderCalls.awaitItem()

    suspend fun awaitSetTapToPayUxConfigurationCall() = setTapToPayUxConfigurationCalls.awaitItem()

    suspend fun awaitRetrieveSetupIntentCall() = retrieveSetupIntentCalls.awaitItem()

    suspend fun awaitCollectSetupIntentPaymentMethodCall() =
        collectSetupIntentPaymentMethodCalls.awaitItem()

    suspend fun awaitConfirmSetupIntentCall() = confirmSetupIntentCalls.awaitItem()

    fun createReader(): Reader {
        return Reader(
            deviceType = DeviceType.TAP_TO_PAY_DEVICE,
        )
    }

    fun createSetupIntent(
        paymentMethod: PaymentMethod? = null,
    ): SetupIntent {
        val paymentMethodIdValue = paymentMethod?.id

        return mock {
            on { paymentMethodId } doReturn paymentMethodIdValue
        }
    }

    fun validate() {
        isInitializedCalls.ensureAllEventsConsumed()
        initTerminalCalls.ensureAllEventsConsumed()
        supportsReadersOfTypeCalls.ensureAllEventsConsumed()
        connectedReaderCalls.ensureAllEventsConsumed()
        discoverReadersCalls.ensureAllEventsConsumed()
        connectReaderCalls.ensureAllEventsConsumed()
        setTapToPayUxConfigurationCalls.ensureAllEventsConsumed()
        retrieveSetupIntentCalls.ensureAllEventsConsumed()
        collectSetupIntentPaymentMethodCalls.ensureAllEventsConsumed()
        confirmSetupIntentCalls.ensureAllEventsConsumed()
    }

    sealed interface ConnectReaderResult {
        data class Success(val reader: Reader) : ConnectReaderResult

        data class Failure(val exception: TerminalException) : ConnectReaderResult
    }

    sealed interface SetupIntentResult {
        data class Success(val setupIntent: SetupIntent) : SetupIntentResult

        data class Failure(val exception: TerminalException) : SetupIntentResult
    }

    class Scenario(
        val isInitialized: Boolean,
        val connectedReader: Reader?,
        val discoveredReaders: List<Reader>,
        val connectReaderResult: ConnectReaderResult,
        val readerSupportResult: ReaderSupportResult,
        val retrieveSetupIntentResult: SetupIntentResult,
        val collectSetupIntentPaymentMethodResult: SetupIntentResult,
        val confirmSetupIntentResult: SetupIntentResult
    )

    class InitTerminalCall(
        val context: Context,
        val tokenProvider: ConnectionTokenProvider,
        val listener: TerminalListener
    )

    class SupportsReadersOfTypeCall(
        val deviceType: DeviceType,
        val discoveryConfiguration: DiscoveryConfiguration,
    )

    class DiscoverReadersCall(
        val config: DiscoveryConfiguration,
    )

    class ConnectReaderCall(
        val reader: Reader,
        val config: ConnectionConfiguration,
    )

    class RetrieveSetupIntentCall(
        val clientSecret: String,
    )

    class CollectSetupIntentPaymentMethodCall(
        val intent: SetupIntent,
        val allowRedisplay: AllowRedisplay,
        val config: CollectSetupIntentConfiguration,
    )

    class ConfirmSetupIntentCall(
        val intent: SetupIntent,
    )

    private fun createMockedTerminalInstance(): Terminal {
        return mock {
            mockSupportsReadersOfType()
            mockConnectedReader()
            mockDiscoverReaders()
            mockConnectReader()
            mockSetTapToPayUxConfiguration()
            mockRetrieveSetupIntent()
            mockCollectSetupIntentPaymentMethod()
            mockConfirmSetupIntent()
        }
    }

    private fun KStubbing<Terminal>.mockSupportsReadersOfType() {
        on {
            supportsReadersOfType(
                deviceType = any<DeviceType>(),
                discoveryConfiguration = any<DiscoveryConfiguration>(),
            )
        } doAnswer { invocation ->
            supportsReadersOfTypeCalls.add(
                SupportsReadersOfTypeCall(
                    deviceType = invocation.getArgument(0),
                    discoveryConfiguration = invocation.getArgument(1),
                )
            )

            scenario.readerSupportResult
        }
    }

    private fun KStubbing<Terminal>.mockConnectedReader() {
        on { connectedReader } doAnswer {
            connectedReaderCalls.add(Unit)

            scenario.connectedReader
        }
    }

    private fun KStubbing<Terminal>.mockDiscoverReaders() {
        on {
            discoverReaders(
                config = any<DiscoveryConfiguration>(),
                discoveryListener = any<DiscoveryListener>(),
                callback = any<Callback>(),
            )
        } doAnswer { invocation ->
            discoverReadersCalls.add(
                DiscoverReadersCall(
                    config = invocation.getArgument(0),
                )
            )

            invocation.getArgument<DiscoveryListener>(1)
                .onUpdateDiscoveredReaders(scenario.discoveredReaders)

            mock()
        }
    }

    private fun KStubbing<Terminal>.mockConnectReader() {
        on {
            connectReader(
                reader = any<Reader>(),
                config = any<ConnectionConfiguration>(),
                connectionCallback = any<ReaderCallback>(),
            )
        } doAnswer { invocation ->
            connectReaderCalls.add(
                ConnectReaderCall(
                    reader = invocation.getArgument(0),
                    config = invocation.getArgument(1),
                )
            )

            val callback = invocation.getArgument<ReaderCallback>(2)

            when (val result = scenario.connectReaderResult) {
                is ConnectReaderResult.Success -> callback.onSuccess(result.reader)
                is ConnectReaderResult.Failure -> callback.onFailure(result.exception)
            }
        }
    }

    private fun KStubbing<Terminal>.mockSetTapToPayUxConfiguration() {
        on { setTapToPayUxConfiguration(any<TapToPayUxConfiguration>()) } doAnswer { invocation ->
            setTapToPayUxConfigurationCalls.add(invocation.getArgument(0))
        }
    }

    private fun KStubbing<Terminal>.mockRetrieveSetupIntent() {
        on {
            retrieveSetupIntent(
                clientSecret = any<String>(),
                callback = any<SetupIntentCallback>(),
            )
        } doAnswer { invocation ->
            retrieveSetupIntentCalls.add(
                RetrieveSetupIntentCall(
                    clientSecret = invocation.getArgument(0),
                )
            )

            val callback = invocation.getArgument<SetupIntentCallback>(1)

            when (val result = scenario.retrieveSetupIntentResult) {
                is SetupIntentResult.Success -> callback.onSuccess(result.setupIntent)
                is SetupIntentResult.Failure -> callback.onFailure(result.exception)
            }
        }
    }

    private fun KStubbing<Terminal>.mockCollectSetupIntentPaymentMethod() {
        on {
            collectSetupIntentPaymentMethod(
                intent = any<SetupIntent>(),
                allowRedisplay = any<AllowRedisplay>(),
                config = any<CollectSetupIntentConfiguration>(),
                callback = any<SetupIntentCallback>(),
            )
        } doAnswer { invocation ->
            collectSetupIntentPaymentMethodCalls.add(
                CollectSetupIntentPaymentMethodCall(
                    intent = invocation.getArgument(0),
                    allowRedisplay = invocation.getArgument(1),
                    config = invocation.getArgument(2),
                )
            )

            val callback = invocation.getArgument<SetupIntentCallback>(3)

            when (val result = scenario.collectSetupIntentPaymentMethodResult) {
                is SetupIntentResult.Success -> callback.onSuccess(result.setupIntent)
                is SetupIntentResult.Failure -> callback.onFailure(result.exception)
            }

            mock()
        }
    }

    private fun KStubbing<Terminal>.mockConfirmSetupIntent() {
        on {
            confirmSetupIntent(
                intent = any<SetupIntent>(),
                callback = any<SetupIntentCallback>(),
            )
        } doAnswer { invocation ->
            confirmSetupIntentCalls.add(
                ConfirmSetupIntentCall(
                    intent = invocation.getArgument(0),
                )
            )

            val callback = invocation.getArgument<SetupIntentCallback>(1)

            when (val result = scenario.confirmSetupIntentResult) {
                is SetupIntentResult.Success -> callback.onSuccess(result.setupIntent)
                is SetupIntentResult.Failure -> callback.onFailure(result.exception)
            }

            mock()
        }
    }

    private class FakeTerminalWrapper(
        private val isInitialized: () -> Boolean,
        private val isInitializedCalls: Turbine<Unit>,
        private val initTerminalCalls: Turbine<InitTerminalCall>,
        private val terminal: Terminal
    ) : TerminalWrapper {
        override fun isInitialized(): Boolean {
            isInitializedCalls.add(Unit)

            return isInitialized.invoke()
        }

        override fun initTerminal(
            context: Context,
            tokenProvider: ConnectionTokenProvider,
            listener: TerminalListener
        ) {
            initTerminalCalls.add(
                InitTerminalCall(
                    context = context,
                    tokenProvider = tokenProvider,
                    listener = listener,
                )
            )
        }

        override fun getInstance(): Terminal {
            return terminal
        }
    }
}
