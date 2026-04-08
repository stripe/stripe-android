package com.stripe.android.common.taptoadd

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.isInstanceOf
import com.stripe.android.paymentelement.TapToAddPreview
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.FakeLogger
import com.stripe.android.testing.FeatureFlagTestRule
import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.external.callable.Callback
import com.stripe.stripeterminal.external.callable.Cancelable
import com.stripe.stripeterminal.external.callable.DiscoveryListener
import com.stripe.stripeterminal.external.callable.ReaderCallback
import com.stripe.stripeterminal.external.models.ConnectionConfiguration
import com.stripe.stripeterminal.external.models.DeviceType
import com.stripe.stripeterminal.external.models.DiscoveryConfiguration
import com.stripe.stripeterminal.external.models.Reader
import com.stripe.stripeterminal.external.models.ReaderSupportResult
import com.stripe.stripeterminal.external.models.TerminalErrorCode
import com.stripe.stripeterminal.external.models.TerminalException
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.KStubbing
import org.mockito.kotlin.any
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFailsWith

@OptIn(TapToAddPreview::class)
@RunWith(RobolectricTestRunner::class)
class DefaultTapToAddConnectionManagerTest {

    @get:Rule
    val featureFlagRule = FeatureFlagTestRule(
        featureFlag = FeatureFlags.enableTapToAdd,
        isEnabled = true,
    )

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val lifecycleOwner = TestLifecycleOwner()

    @Test
    fun `init initializes terminal when not already initialized`() = test(
        isInitialized = false,
        autoCheckIsInitializedCall = false,
    ) {
        assertThat(wrapperScenario.isInitializedCalls.awaitItem()).isNotNull()

        val initTerminalCall = wrapperScenario.initTerminalCalls.awaitItem()

        assertThat(initTerminalCall.context).isEqualTo(context)
        assertThat(initTerminalCall.listener).isEqualTo(manager)
    }

    @Test
    fun `init does not initialize terminal when already initialized`() = test(
        isInitialized = true,
        autoCheckIsInitializedCall = false,
    ) {
        assertThat(wrapperScenario.isInitializedCalls.awaitItem()).isNotNull()
        wrapperScenario.initTerminalCalls.expectNoEvents()
    }

    @Test
    fun `isSupported returns true when terminal supports tap to add`() = test(
        terminalInstance = mock {
            mockSupportedReaderResult(ReaderSupportResult.Supported)
        }
    ) {
        assertThat(manager.isSupported).isTrue()
    }

    @Test
    fun `isSupported calls returns false is feature flag is disabled`() = test(
        isSimulated = true,
        terminalInstance = mock {
            mockSupportedReaderResult(ReaderSupportResult.Supported)
        }
    ) {
        featureFlagRule.setEnabled(false)

        assertThat(manager.isSupported).isFalse()

        verify(terminalInstance, never()).supportsReadersOfType(
            deviceType = any(),
            discoveryConfiguration = any(),
        )
    }

    @Test
    fun `isSupported calls supportsReadersOfType with isSimulated param on config set to true`() = test(
        isSimulated = true,
        terminalInstance = mock {
            mockSupportedReaderResult(ReaderSupportResult.Supported)
        }
    ) {
        assertThat(manager.isSupported).isTrue()

        verify(terminalInstance).supportsReadersOfType(
            deviceType = DeviceType.TAP_TO_PAY_DEVICE,
            discoveryConfiguration = DiscoveryConfiguration.TapToPayDiscoveryConfiguration(isSimulated = true)
        )
    }

    @Test
    fun `isSupported calls supportsReadersOfType with isSimulated param on config set to false`() = test(
        isSimulated = false,
        terminalInstance = mock {
            mockSupportedReaderResult(ReaderSupportResult.Supported)
        }
    ) {
        assertThat(manager.isSupported).isTrue()

        verify(terminalInstance).supportsReadersOfType(
            deviceType = DeviceType.TAP_TO_PAY_DEVICE,
            discoveryConfiguration = DiscoveryConfiguration.TapToPayDiscoveryConfiguration(isSimulated = false)
        )
    }

    @Test
    fun `isSupported returns false when terminal does not support tap to add`() = test(
        terminalInstance = mock {
            mockSupportedReaderResult(ReaderSupportResult.NotSupported(IllegalStateException("Not supported!")))
        }
    ) {
        assertThat(manager.isSupported).isFalse()
    }

    @Test
    fun `connect does nothing when not supported`() = test(
        terminalInstance = mock {
            mockSupportedReaderResult(ReaderSupportResult.NotSupported(IllegalStateException("Not supported!")))
            mockReaderCall()
        }
    ) {
        assertFailsWith<IllegalStateException> {
            manager.connect()
        }

        verify(terminalInstance, never()).discoverReaders(any(), any(), any())
    }

    @Test
    fun `connect does nothing when already connected`() = test(
        terminalInstance = mock {
            mockSupportedReaderResult(ReaderSupportResult.Supported)
            mockReaderCall(Reader())
        }
    ) {
        manager.connect()

        verify(terminalInstance, never()).discoverReaders(any(), any(), any())
    }

    @Test
    fun `connect initiates discovery when supported and not connected`() = test(
        terminalInstance = mock {
            mockSupportedReaderResult(ReaderSupportResult.Supported)
            mockReaderCall()
            mockDiscoverCall()
        }
    ) {
        val connectionJob = testScope.async { manager.connect() }

        verify(terminalInstance).discoverReaders(
            any<DiscoveryConfiguration.TapToPayDiscoveryConfiguration>(),
            any<DiscoveryListener>(),
            any<Callback>(),
        )

        connectionJob.cancel()
    }

    @Test
    fun `connect reports error when discover readers cancel fails after coroutine cancelled`() = test(
        terminalInstance = mock {
            mockSupportedReaderResult(ReaderSupportResult.Supported)
            mockReaderCall()
            mockDiscoverCall(
                mock<Cancelable> {
                    on { cancel(any()) } doAnswer { invocation ->
                        val callback = invocation.getArgument<Callback>(0)
                        callback.onFailure(
                            TerminalException(
                                errorCode = TerminalErrorCode.CANCEL_FAILED,
                                errorMessage = "Failed to cancel discovery",
                            )
                        )
                    }
                }
            )
        }
    ) {
        val connectionJob = testScope.async { manager.connect() }

        verify(terminalInstance).discoverReaders(
            any<DiscoveryConfiguration.TapToPayDiscoveryConfiguration>(),
            any<DiscoveryListener>(),
            any<Callback>(),
        )

        connectionJob.cancel()

        val reportCall = errorReporter.awaitCall()
        assertThat(reportCall.errorEvent)
            .isEqualTo(ErrorReporter.UnexpectedErrorEvent.TAP_TO_ADD_DISCOVER_READERS_CANCEL_FAILURE)
        assertThat(reportCall.stripeException?.cause).isInstanceOf(TerminalException::class.java)
        assertThat(reportCall.additionalNonPiiParams).isEmpty()
    }

    @Test
    fun `Multiple connect calls only result in one discovery`() = test(
        terminalInstance = mock {
            mockSupportedReaderResult(ReaderSupportResult.Supported)
            mockReaderCall()
            mockDiscoverCall()
        }
    ) {
        val jobs = arrayOf(
            testScope.async { manager.connect() },
            testScope.async { manager.connect() },
            testScope.async { manager.connect() },
            testScope.async { manager.connect() }
        )

        verify(terminalInstance, times(1)).discoverReaders(
            any<DiscoveryConfiguration.TapToPayDiscoveryConfiguration>(),
            any<DiscoveryListener>(),
            any<Callback>(),
        )

        jobs.forEach {
            it.cancel()
        }
    }

    @Test
    fun `connect reports error on expected discovery call permission failure`() {
        val exception = SecurityException("Permission failure!")

        test(
            terminalInstance = mock {
                mockSupportedReaderResult(ReaderSupportResult.Supported)
                mockReaderCall()
                mockDiscoverFailure(exception)
            }
        ) {
            assertFailsWith<SecurityException> {
                manager.connect()
            }

            verify(terminalInstance).discoverReaders(
                any<DiscoveryConfiguration.TapToPayDiscoveryConfiguration>(),
                any<DiscoveryListener>(),
                any<Callback>(),
            )

            val errorReportCall = errorReporter.awaitCall()

            assertThat(logger.warningLogs).containsExactly("TapToAddConnectionError: $exception")
            assertThat(errorReportCall.errorEvent)
                .isEqualTo(ErrorReporter.UnexpectedErrorEvent.TAP_TO_ADD_LOCATION_PERMISSIONS_FAILURE)
            assertThat(errorReportCall.stripeException?.message).isEqualTo("Permission failure!")
            assertThat(errorReportCall.additionalNonPiiParams).isEmpty()
        }
    }

    @Test
    fun `connect initiates reader connection after discovering reader`() {
        test(
            terminalInstance = mock {
                mockSupportedReaderResult(ReaderSupportResult.Supported)
                mockReaderCall()
                mockDiscoverCall(
                    mock<Cancelable> {
                        on { isCompleted } doReturn false
                    }
                )
            }
        ) {
            val connectionJob = testScope.async { manager.connect() }

            val listenerCaptor = argumentCaptor<DiscoveryListener>()

            verify(terminalInstance).discoverReaders(
                any<DiscoveryConfiguration.TapToPayDiscoveryConfiguration>(),
                listenerCaptor.capture(),
                any<Callback>(),
            )

            val reader = Reader()

            listenerCaptor.firstValue.onUpdateDiscoveredReaders(listOf(reader))

            verify(terminalInstance).connectReader(
                eq(reader),
                argWhere { config ->
                    config is ConnectionConfiguration.TapToPayConnectionConfiguration &&
                        config.autoReconnectOnUnexpectedDisconnect &&
                        config.tapToPayReaderListener == manager
                },
                any<ReaderCallback>(),
            )

            connectionJob.cancel()
        }
    }

    @Test
    fun `connect reports unexpected reader not found error when no readers are returned`() = test(
        terminalInstance = mock {
            mockSupportedReaderResult(ReaderSupportResult.Supported)
            mockReaderCall()
            mockDiscoverCall(
                mock<Cancelable> {
                    on { isCompleted } doReturn false
                }
            )
        }
    ) {
       val connectionResult = testScope.async {
            assertFailsWith<IllegalStateException> {
                manager.connect()
            }
        }

        val listenerCaptor = argumentCaptor<DiscoveryListener>()

        verify(terminalInstance).discoverReaders(
            any<DiscoveryConfiguration.TapToPayDiscoveryConfiguration>(),
            listenerCaptor.capture(),
            any<Callback>(),
        )

        listenerCaptor.firstValue.onUpdateDiscoveredReaders(emptyList())

        val errorReportCall = errorReporter.awaitCall()

        assertThat(errorReportCall.errorEvent)
            .isEqualTo(ErrorReporter.UnexpectedErrorEvent.TAP_TO_ADD_NO_READER_FOUND)
        assertThat(logger.warningLogs).containsExactly(
            "TapToAddConnectionError: java.lang.IllegalStateException: No reader found!"
        )

        assertThat(connectionResult.await()).isInstanceOf<IllegalStateException>()
    }

    @Test
    fun `connect reports success event on successful reader discovery`() = test(
        terminalInstance = mock {
            mockSupportedReaderResult(ReaderSupportResult.Supported)
            mockReaderCall()
            mockDiscoverCall(
                mock<Cancelable> {
                    on { isCompleted } doReturn false
                }
            )
        }
    ) {
        val connectionJob = testScope.async { manager.connect() }

        val callbackCaptor = argumentCaptor<Callback>()

        verify(terminalInstance).discoverReaders(
            any<DiscoveryConfiguration.TapToPayDiscoveryConfiguration>(),
            any<DiscoveryListener>(),
            callbackCaptor.capture(),
        )

        callbackCaptor.firstValue.onSuccess()

        val successReportCall = errorReporter.awaitCall()

        assertThat(successReportCall.errorEvent)
            .isEqualTo(ErrorReporter.SuccessEvent.TAP_TO_ADD_DISCOVER_READERS_CALL_SUCCESS)

        connectionJob.cancel()
    }

    @Test
    fun `connect reports failure event on failed reader discovery`() = test(
        terminalInstance = mock {
            mockSupportedReaderResult(ReaderSupportResult.Supported)
            mockReaderCall()
            mockDiscoverCall(
                mock<Cancelable> {
                    on { isCompleted } doReturn false
                }
            )
        }
    ) {
        val connectionResult = testScope.async {
            assertFailsWith<TerminalException> { manager.connect() }
        }

        val callbackCaptor = argumentCaptor<Callback>()

        verify(terminalInstance).discoverReaders(
            any<DiscoveryConfiguration.TapToPayDiscoveryConfiguration>(),
            any<DiscoveryListener>(),
            callbackCaptor.capture(),
        )

        val exception = TerminalException(
            errorCode = TerminalErrorCode.CANCEL_FAILED,
            errorMessage = "Something went wrong!"
        )

        callbackCaptor.firstValue.onFailure(exception)

        val successReportCall = errorReporter.awaitCall()

        assertThat(successReportCall.errorEvent)
            .isEqualTo(ErrorReporter.ExpectedErrorEvent.TAP_TO_ADD_DISCOVER_READERS_CALL_FAILURE)
        assertThat(successReportCall.stripeException?.cause).isEqualTo(exception)
        assertThat(logger.warningLogs).containsExactly("TapToAddConnectionError: $exception")

        assertThat(connectionResult.await()).isEqualTo(exception)
    }

    @Test
    fun `connect reports success event on successful reader connection`() {
        test(
            terminalInstance = mock {
                mockSupportedReaderResult(ReaderSupportResult.Supported)
                mockReaderCall()
                mockDiscoverCall(
                    mock<Cancelable> {
                        on { isCompleted } doReturn false
                    }
                )
            }
        ) {
            val connectionResult = testScope.async { manager.connect() }

            val reader = Reader()

            captureDiscoveryListener().onUpdateDiscoveredReaders(listOf(reader))
            captureReaderCallback().onSuccess(reader)

            val successReportCall = errorReporter.awaitCall()

            assertThat(successReportCall.errorEvent)
                .isEqualTo(ErrorReporter.SuccessEvent.TAP_TO_ADD_CONNECT_READER_CALL_SUCCESS)

            assertThat(connectionResult).isNotNull()
        }
    }

    @Test
    fun `connect reports failure event on failed reader connection`() {
        test(
            terminalInstance = mock {
                mockSupportedReaderResult(ReaderSupportResult.Supported)
                mockReaderCall()
                mockDiscoverCall(
                    mock<Cancelable> {
                        on { isCompleted } doReturn false
                    }
                )
            }
        ) {
            val connectionResult = testScope.async {
                assertFailsWith<TerminalException> { manager.connect() }
            }

            val reader = Reader()
            val exception = TerminalException(
                errorCode = TerminalErrorCode.CANCEL_FAILED,
                errorMessage = "Something went wrong!"
            )

            captureDiscoveryListener().onUpdateDiscoveredReaders(listOf(reader))
            captureReaderCallback().onFailure(exception)

            val errorCall = errorReporter.awaitCall()

            assertThat(errorCall.errorEvent)
                .isEqualTo(ErrorReporter.ExpectedErrorEvent.TAP_TO_ADD_CONNECT_READER_CALL_FAILURE)
            assertThat(errorCall.stripeException?.cause).isEqualTo(exception)
            assertThat(logger.warningLogs)
                .containsExactly("TapToAddConnectionError: $exception")

            assertThat(connectionResult.await()).isEqualTo(exception)
        }
    }

    @Test
    fun `connect completes successfully on discovery if already connected to reader`() {
        val connectedReader = Reader()
        val exception = TerminalException(
            errorCode = TerminalErrorCode.ALREADY_CONNECTED_TO_READER,
            errorMessage = "Already connected!"
        )

        test(
            terminalInstance = mock {
                mockSupportedReaderResult(ReaderSupportResult.Supported)
                mockReaderCall()
                mockDiscoverCall()
            }
        ) {
            val connectionResult = testScope.async { manager.connect() }

            whenever(terminalInstance.connectedReader).thenReturn(connectedReader)

            captureDiscoveryCallback().onFailure(exception)

            assertThat(connectionResult.await()).isNotNull()

            val errorCall = errorReporter.awaitCall()

            assertThat(errorCall.errorEvent)
                .isEqualTo(ErrorReporter.SuccessEvent.TAP_TO_ADD_DISCOVER_READERS_CALL_SUCCESS)
        }
    }

    @Test
    fun `connect completes successfully on connect if already connected to reader`() {
        val connectedReader = Reader()
        val exception = TerminalException(
            errorCode = TerminalErrorCode.ALREADY_CONNECTED_TO_READER,
            errorMessage = "Already connected!"
        )

        test(
            terminalInstance = mock {
                mockSupportedReaderResult(ReaderSupportResult.Supported)
                mockReaderCall()
                mockDiscoverCall(
                    mock<Cancelable> {
                        on { isCompleted } doReturn false
                    }
                )
            }
        ) {
            val job = testScope.async { manager.connect() }

            whenever(terminalInstance.connectedReader).thenReturn(connectedReader)

            captureDiscoveryListener().onUpdateDiscoveredReaders(listOf(connectedReader))
            captureReaderCallback().onFailure(exception)

            assertThat(job.await()).isNotNull()

            val errorCall = errorReporter.awaitCall()

            assertThat(errorCall.errorEvent)
                .isEqualTo(ErrorReporter.SuccessEvent.TAP_TO_ADD_CONNECT_READER_CALL_SUCCESS)
        }
    }

    @Test
    fun `connect completes successfully when discovery and connect succeed`() {
        test(
            terminalInstance = mock {
                mockSupportedReaderResult(ReaderSupportResult.Supported)
                mockReaderCall()
                mockDiscoverCall(
                    mock<Cancelable> {
                        on { isCompleted } doReturn false
                    }
                )
            }
        ) {
            val job = testScope.async { manager.connect() }

            val reader = Reader()

            captureDiscoveryListener().onUpdateDiscoveredReaders(listOf(reader))
            captureReaderCallback().onSuccess(reader)

            assertThat(errorReporter.awaitCall()).isNotNull()

            job.await()
        }
    }

    @Test
    fun `connect throws when connection fails`() {
        test(
            terminalInstance = mock {
                mockSupportedReaderResult(ReaderSupportResult.Supported)
                mockReaderCall()
                mockDiscoverCall(
                    mock<Cancelable> {
                        on { isCompleted } doReturn false
                    }
                )
            }
        ) {
            val job = testScope.async {
                assertFailsWith<TerminalException> {
                    manager.connect()
                }
            }

            val reader = Reader()
            val exception = TerminalException(
                errorCode = TerminalErrorCode.CANCEL_FAILED,
                errorMessage = "Something went wrong!"
            )

            captureDiscoveryListener().onUpdateDiscoveredReaders(listOf(reader))
            captureReaderCallback().onFailure(exception)

            assertThat(errorReporter.awaitCall()).isNotNull()

            assertThat(job.await()).isEqualTo(exception)
        }
    }

    private fun KStubbing<Terminal>.mockSupportedReaderResult(
        result: ReaderSupportResult,
    ) {
        on {
            supportsReadersOfType(
                deviceType = eq(DeviceType.TAP_TO_PAY_DEVICE),
                discoveryConfiguration = any<DiscoveryConfiguration.TapToPayDiscoveryConfiguration>(),
            )
        } doReturn result
    }

    private fun KStubbing<Terminal>.mockDiscoverCall(
        result: Cancelable = mock()
    ) {
        on {
            discoverReaders(
                any<DiscoveryConfiguration.TapToPayDiscoveryConfiguration>(),
                any<DiscoveryListener>(),
                any<Callback>(),
            )
        } doReturn result
    }

    private fun KStubbing<Terminal>.mockDiscoverFailure(
        exception: Throwable
    ) {
        on {
            discoverReaders(
                any<DiscoveryConfiguration.TapToPayDiscoveryConfiguration>(),
                any<DiscoveryListener>(),
                any<Callback>(),
            )
        } doThrow exception
    }

    private fun Scenario.captureDiscoveryCallback(): Callback {
        val discoveryCallbackCaptor = argumentCaptor<Callback>()

        verify(terminalInstance).discoverReaders(
            any<DiscoveryConfiguration.TapToPayDiscoveryConfiguration>(),
            any<DiscoveryListener>(),
            discoveryCallbackCaptor.capture(),
        )

        return discoveryCallbackCaptor.firstValue
    }

    private fun KStubbing<Terminal>.mockReaderCall(
        reader: Reader? = null
    ) {
        on { connectedReader } doReturn reader
    }

    private fun Scenario.captureDiscoveryListener(): DiscoveryListener {
        val listenerCaptor = argumentCaptor<DiscoveryListener>()

        verify(terminalInstance).discoverReaders(
            any<DiscoveryConfiguration.TapToPayDiscoveryConfiguration>(),
            listenerCaptor.capture(),
            any<Callback>(),
        )

        return listenerCaptor.firstValue
    }

    private fun Scenario.captureReaderCallback(): ReaderCallback {
        val readerCaptor = argumentCaptor<ReaderCallback>()

        verify(terminalInstance).connectReader(
            any<Reader>(),
            any<ConnectionConfiguration.TapToPayConnectionConfiguration>(),
            readerCaptor.capture(),
        )

        return readerCaptor.firstValue
    }

    private fun test(
        isInitialized: Boolean = true,
        terminalInstance: Terminal = mock(),
        autoCheckIsInitializedCall: Boolean = true,
        isSimulated: Boolean = true,
        block: suspend Scenario.() -> Unit
    ) = runTest(UnconfinedTestDispatcher()) {
        val errorReporter = FakeErrorReporter()
        val logger = FakeLogger()

        TestTerminalWrapper.test(
            isInitialized = isInitialized,
            terminalInstance = terminalInstance,
        ) {
            block(
                Scenario(
                    manager = DefaultTapToAddConnectionManager(
                        applicationContext = context,
                        workContext = testDispatcher,
                        terminalWrapper = wrapper,
                        errorReporter = errorReporter,
                        logger = logger,
                        isSimulatedProvider = object : TapToAddIsSimulatedProvider {
                            override fun get(): Boolean = isSimulated
                        },
                        paymentConfiguration = { PaymentConfiguration(publishableKey = "pk_test") }
                    ),
                    terminalInstance = terminalInstance,
                    errorReporter = errorReporter,
                    testScope = this@runTest,
                    logger = logger,
                    wrapperScenario = this
                )
            )

            if (autoCheckIsInitializedCall) {
                assertThat(isInitializedCalls.awaitItem()).isNotNull()
            }
        }

        errorReporter.ensureAllEventsConsumed()

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    private class Scenario(
        val testScope: TestScope,
        val manager: TapToAddConnectionManager,
        val logger: FakeLogger,
        val terminalInstance: Terminal,
        val errorReporter: FakeErrorReporter,
        val wrapperScenario: TestTerminalWrapper.Scenario
    )
}
