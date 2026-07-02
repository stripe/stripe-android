package com.stripe.android.common.nfcscan.scanner

import android.nfc.Tag
import androidx.appcompat.app.AppCompatActivity
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.nfcscan.hardware.FakeNfcHardwareDelegate
import com.stripe.android.testing.CoroutineTestRule
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock

internal class DefaultNfcCardScannerTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(dispatcher)

    @Test
    fun `start emits Scanning then Complete when card read succeeds`() = runScenario(
        cardData = ScannedCardData(
            cardNumber = "4242424242424242",
            expirationMonth = 12,
            expirationYear = 2030,
        ),
    ) {
        scanner.state.test {
            scanner.start(activity)

            val startCall = fakeHardwareDelegate.startCalls.awaitItem()
            startCall.onTagDiscovered.invoke(tag)

            assertThat(awaitItem()).isEqualTo(NfcCardScanner.State.Scanning)
            assertThat(awaitItem()).isEqualTo(
                NfcCardScanner.State.Complete(
                    ScannedCardData(
                        cardNumber = "4242424242424242",
                        expirationMonth = 12,
                        expirationYear = 2030,
                    ),
                ),
            )
        }

        assertThat(fakeTransceiverFactory.createCalls.awaitItem()).isEqualTo(tag)

        val transceiver = requireNotNull(fakeTransceiver)

        assertThat(transceiver.openCalls.awaitItem()).isNotNull()
        assertThat(fakeCardReader.readCardCalls.awaitItem()).isEqualTo(fakeTransceiver)
        assertThat(transceiver.closeCalls.awaitItem()).isNotNull()
    }

    @Test
    fun `start emits Failed when card reader fails`() = runScenario(
        cardReadError = IllegalStateException("read failed"),
    ) {
        scanner.state.test {
            scanner.start(activity)

            val startCall = fakeHardwareDelegate.startCalls.awaitItem()
            startCall.onTagDiscovered.invoke(tag)

            assertThat(awaitItem()).isEqualTo(NfcCardScanner.State.Scanning)
            val failedState = awaitItem() as NfcCardScanner.State.Failed
            assertThat(failedState.error).isInstanceOf(IllegalStateException::class.java)
        }

        assertThat(fakeTransceiverFactory.createCalls.awaitItem()).isEqualTo(tag)

        val transceiver = requireNotNull(fakeTransceiver)

        assertThat(transceiver.openCalls.awaitItem()).isNotNull()
        assertThat(fakeCardReader.readCardCalls.awaitItem()).isEqualTo(fakeTransceiver)
        assertThat(transceiver.closeCalls.awaitItem()).isNotNull()
    }

    @Test
    fun `start does not emit state when transceiver factory returns null`() = runScenario(
        transceiver = null,
    ) {
        scanner.state.test {
            scanner.start(activity)

            val startCall = fakeHardwareDelegate.startCalls.awaitItem()
            startCall.onTagDiscovered.invoke(tag)

            expectNoEvents()
        }

        assertThat(fakeTransceiverFactory.createCalls.awaitItem()).isEqualTo(tag)
    }

    @Test
    fun `start emits Failed when transceiver open fails`() = runScenario(
        openException = IOException("open failed"),
    ) {
        scanner.state.test {
            scanner.start(activity)

            val startCall = fakeHardwareDelegate.startCalls.awaitItem()
            startCall.onTagDiscovered.invoke(tag)

            assertThat(awaitItem()).isEqualTo(NfcCardScanner.State.Scanning)
            val failedState = awaitItem() as NfcCardScanner.State.Failed
            assertThat(failedState.error).isInstanceOf(IOException::class.java)
            assertThat(failedState.error.message).isEqualTo("open failed")
        }

        assertThat(fakeTransceiverFactory.createCalls.awaitItem()).isEqualTo(tag)

        val transceiver = requireNotNull(fakeTransceiver)

        assertThat(transceiver.openCalls.awaitItem()).isNotNull()
    }

    @Test
    fun `start emits Failed when transceiver open throws SecurityException`() = runScenario(
        openException = SecurityException("nfc permission denied"),
    ) {
        scanner.state.test {
            scanner.start(activity)

            val startCall = fakeHardwareDelegate.startCalls.awaitItem()
            startCall.onTagDiscovered.invoke(tag)

            assertThat(awaitItem()).isEqualTo(NfcCardScanner.State.Scanning)
            val failedState = awaitItem() as NfcCardScanner.State.Failed
            assertThat(failedState.error).isInstanceOf(SecurityException::class.java)
            assertThat(failedState.error.message).isEqualTo("nfc permission denied")
        }

        assertThat(fakeTransceiverFactory.createCalls.awaitItem()).isEqualTo(tag)
        assertThat(requireNotNull(fakeTransceiver).openCalls.awaitItem()).isNotNull()
    }

    @Test
    fun `start emits Failed when transceiver close fails`() = runScenario(
        cardData = ScannedCardData(
            cardNumber = "4242424242424242",
            expirationMonth = 12,
            expirationYear = 2030,
        ),
        closeException = IOException("close failed"),
    ) {
        scanner.state.test {
            scanner.start(activity)

            val startCall = fakeHardwareDelegate.startCalls.awaitItem()
            startCall.onTagDiscovered.invoke(tag)

            assertThat(awaitItem()).isEqualTo(NfcCardScanner.State.Scanning)
            val failedState = awaitItem() as NfcCardScanner.State.Failed
            assertThat(failedState.error).isInstanceOf(IOException::class.java)
            assertThat(failedState.error.message).isEqualTo("close failed")
        }

        assertThat(fakeTransceiverFactory.createCalls.awaitItem()).isEqualTo(tag)

        val transceiver = requireNotNull(fakeTransceiver)

        assertThat(transceiver.openCalls.awaitItem()).isNotNull()
        assertThat(fakeCardReader.readCardCalls.awaitItem()).isEqualTo(fakeTransceiver)
        assertThat(transceiver.closeCalls.awaitItem()).isNotNull()
    }

    @Test
    fun `start passes activity to hardware delegate`() = runScenario {
        scanner.start(activity)

        val startCall = fakeHardwareDelegate.startCalls.awaitItem()

        assertThat(startCall.activity).isEqualTo(activity)
    }

    @Test
    fun `start does not emit state before tag is discovered`() = runScenario {
        scanner.state.test {
            scanner.start(activity)

            fakeHardwareDelegate.startCalls.awaitItem()

            expectNoEvents()
        }
    }

    @Test
    fun `start handles multiple tag discoveries`() = runScenario(
        cardData = ScannedCardData(
            cardNumber = "4242424242424242",
            expirationMonth = 12,
            expirationYear = 2030,
        ),
    ) {
        scanner.state.test {
            scanner.start(activity)

            val startCall = fakeHardwareDelegate.startCalls.awaitItem()

            startCall.onTagDiscovered.invoke(tag)
            assertThat(awaitItem()).isEqualTo(NfcCardScanner.State.Scanning)
            assertThat(awaitItem()).isInstanceOf(NfcCardScanner.State.Complete::class.java)

            startCall.onTagDiscovered.invoke(tag)
            assertThat(awaitItem()).isEqualTo(NfcCardScanner.State.Scanning)
            assertThat(awaitItem()).isInstanceOf(NfcCardScanner.State.Complete::class.java)
        }

        assertThat(fakeTransceiverFactory.createCalls.awaitItem()).isEqualTo(tag)
        assertThat(fakeTransceiverFactory.createCalls.awaitItem()).isEqualTo(tag)

        val transceiver = requireNotNull(fakeTransceiver)

        assertThat(transceiver.openCalls.awaitItem()).isNotNull()
        assertThat(fakeCardReader.readCardCalls.awaitItem()).isEqualTo(fakeTransceiver)
        assertThat(transceiver.closeCalls.awaitItem()).isNotNull()
        assertThat(transceiver.openCalls.awaitItem()).isNotNull()
        assertThat(fakeCardReader.readCardCalls.awaitItem()).isEqualTo(fakeTransceiver)
        assertThat(transceiver.closeCalls.awaitItem()).isNotNull()
    }

    private fun runScenario(
        isHardwareAvailable: Boolean = true,
        cardData: ScannedCardData? = null,
        cardReadError: Throwable? = null,
        openException: Throwable? = null,
        closeException: Throwable? = null,
        transceiver: NfcTagTransceiver? = FakeNfcTagTransceiver(
            openException = openException,
            closeException = closeException,
        ),
        block: suspend Scenario.() -> Unit,
    ) = runTest(dispatcher) {
        val fakeHardwareDelegate = FakeNfcHardwareDelegate(result = isHardwareAvailable)
        val fakeTransceiverFactory = FakeNfcTagTransceiverFactory(transceiver = transceiver)
        val fakeTransceiver = transceiver as? FakeNfcTagTransceiver
        val fakeCardReader = FakeNfcCardReader(
            result = when {
                cardData != null -> Result.success(cardData)
                cardReadError != null -> Result.failure(cardReadError)
                else -> Result.failure(NotImplementedError())
            },
        )
        val activity = mock<AppCompatActivity>()
        val tag = mock<Tag>()

        val scanner = DefaultNfcCardScanner(
            hardwareDelegate = fakeHardwareDelegate,
            cardReader = fakeCardReader,
            transceiverFactory = fakeTransceiverFactory,
            viewModelScope = CoroutineScope(dispatcher),
            workContext = dispatcher,
        )

        Scenario(
            scanner = scanner,
            activity = activity,
            tag = tag,
            fakeHardwareDelegate = fakeHardwareDelegate,
            fakeTransceiverFactory = fakeTransceiverFactory,
            fakeCardReader = fakeCardReader,
            fakeTransceiver = fakeTransceiver,
        ).block()

        fakeHardwareDelegate.ensureAllEventsConsumed()
        fakeTransceiverFactory.ensureAllEventsConsumed()
        fakeCardReader.ensureAllEventsConsumed()
        fakeTransceiver?.ensureAllEventsConsumed()
    }

    private class Scenario(
        val scanner: DefaultNfcCardScanner,
        val activity: AppCompatActivity,
        val tag: Tag,
        val fakeHardwareDelegate: FakeNfcHardwareDelegate,
        val fakeTransceiverFactory: FakeNfcTagTransceiverFactory,
        val fakeCardReader: FakeNfcCardReader,
        val fakeTransceiver: FakeNfcTagTransceiver?,
    )
}
