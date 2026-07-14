package com.stripe.android.common.nfcscan.scanner

import android.nfc.Tag
import androidx.appcompat.app.AppCompatActivity
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.nfcscan.hardware.FakeNfcHardwareDelegate
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentsheet.R
import com.stripe.android.testing.CoroutineTestRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import java.io.IOException

internal class DefaultNfcCardScannerTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(dispatcher)

    @Test
    fun `scanner emits Scanning then Complete when card read & validator succeeds`() = runScenario(
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
        assertThat(fakeCardValidator.validateCalls.awaitItem()).isEqualTo(
            ScannedCardData(
                cardNumber = "4242424242424242",
                expirationMonth = 12,
                expirationYear = 2030,
            ),
        )
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
            assertThat(awaitItem()).isEqualTo(
                NfcCardScanner.State.Failed(
                    error = NfcCardScanner.Error(
                        code = "unknown",
                        userMessage = R.string.stripe_tap_to_add_card_default_error_action.resolvableString,
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
            assertThat(awaitItem()).isEqualTo(
                NfcCardScanner.State.Failed(
                    error = NfcCardScanner.Error(
                        code = "unknown",
                        userMessage = R.string.stripe_tap_to_add_card_default_error_action.resolvableString,
                    ),
                ),
            )
        }

        assertThat(fakeTransceiverFactory.createCalls.awaitItem()).isEqualTo(tag)

        val transceiver = requireNotNull(fakeTransceiver)

        assertThat(transceiver.openCalls.awaitItem()).isNotNull()
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
            assertThat(failedState.error).isEqualTo(
                NfcCardScanner.Error(
                    code = "unknown",
                    userMessage = R.string.stripe_tap_to_add_card_default_error_action.resolvableString,
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
    fun `scanner emits Failed when card validation fails`() = runScenario(
        cardData = ScannedCardData(
            cardNumber = "4242424242424242",
            expirationMonth = 12,
            expirationYear = 2030,
        ),
        validationResult = NfcCardValidator.Result.Invalid(
            errorCode = "cardUnsupportedByMerchant",
            userMessage = R.string.stripe_nfc_scan_unsupported_card.resolvableString,
        ),
    ) {
        scanner.state.test {
            scanner.start(activity)

            val startCall = fakeHardwareDelegate.startCalls.awaitItem()
            startCall.onTagDiscovered.invoke(tag)

            assertThat(awaitItem()).isEqualTo(NfcCardScanner.State.Scanning)
            assertThat(awaitItem()).isEqualTo(
                NfcCardScanner.State.Failed(
                    error = NfcCardScanner.Error(
                        code = "cardUnsupportedByMerchant",
                        userMessage = R.string.stripe_nfc_scan_unsupported_card.resolvableString,
                    ),
                ),
            )
        }

        assertThat(fakeTransceiverFactory.createCalls.awaitItem()).isEqualTo(tag)

        val transceiver = requireNotNull(fakeTransceiver)

        assertThat(transceiver.openCalls.awaitItem()).isNotNull()
        assertThat(fakeCardReader.readCardCalls.awaitItem()).isEqualTo(fakeTransceiver)
        assertThat(transceiver.closeCalls.awaitItem()).isNotNull()
        assertThat(fakeCardValidator.validateCalls.awaitItem()).isEqualTo(
            ScannedCardData(
                cardNumber = "4242424242424242",
                expirationMonth = 12,
                expirationYear = 2030,
            ),
        )
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
        validationResult: NfcCardValidator.Result = NfcCardValidator.Result.Validated,
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
        val fakeCardValidator = FakeNfcCardValidator(result = validationResult)
        val activity = mock<AppCompatActivity>()
        val tag = mock<Tag>()

        val scanner = DefaultNfcCardScanner(
            hardwareDelegate = fakeHardwareDelegate,
            cardReader = fakeCardReader,
            cardValidator = fakeCardValidator,
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
            fakeCardValidator = fakeCardValidator,
            fakeTransceiver = fakeTransceiver,
        ).block()

        fakeHardwareDelegate.ensureAllEventsConsumed()
        fakeTransceiverFactory.ensureAllEventsConsumed()
        fakeCardReader.ensureAllEventsConsumed()
        fakeCardValidator.ensureAllEventsConsumed()
        fakeTransceiver?.ensureAllEventsConsumed()
    }

    private class Scenario(
        val scanner: DefaultNfcCardScanner,
        val activity: AppCompatActivity,
        val tag: Tag,
        val fakeHardwareDelegate: FakeNfcHardwareDelegate,
        val fakeTransceiverFactory: FakeNfcTagTransceiverFactory,
        val fakeCardReader: FakeNfcCardReader,
        val fakeCardValidator: FakeNfcCardValidator,
        val fakeTransceiver: FakeNfcTagTransceiver?,
    )
}
