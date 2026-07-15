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

internal class DefaultNfcCardScannerTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(dispatcher)

    @Test
    fun `scanner emits Scanning then Complete when card read & validator succeeds`() = runScenario(
        cardReadResult = NfcCardReader.Result.Found(
            scannedCardData = ScannedCardData(
                cardNumber = "4242424242424242",
                expirationMonth = 12,
                expirationYear = 2030,
            ),
        )
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
        assertThat(fakeCardReader.readCardCalls.awaitItem()).isEqualTo(fakeTransceiver)
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
        cardReadResult = NfcCardReader.Result.Error(
            errorCode = "cardUnsupportedByNfc",
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
                        code = "cardUnsupportedByNfc",
                        userMessage = R.string.stripe_nfc_scan_unsupported_card.resolvableString,
                    ),
                ),
            )
        }

        assertThat(fakeTransceiverFactory.createCalls.awaitItem()).isEqualTo(tag)
        assertThat(fakeCardReader.readCardCalls.awaitItem()).isEqualTo(fakeTransceiver)
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
    fun `scanner emits Failed when card validation fails`() = runScenario(
        cardReadResult = NfcCardReader.Result.Found(
            scannedCardData = ScannedCardData(
                cardNumber = "4242424242424242",
                expirationMonth = 12,
                expirationYear = 2030,
            ),
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
        assertThat(fakeCardReader.readCardCalls.awaitItem()).isEqualTo(fakeTransceiver)
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
        transceiver: FakeNfcTagTransceiver? = FakeNfcTagTransceiver(),
        cardReadResult: NfcCardReader.Result = NfcCardReader.Result.Error(
            errorCode = "notImplemented",
            userMessage = R.string.stripe_tap_to_add_card_default_error_action.resolvableString,
        ),
        validationResult: NfcCardValidator.Result = NfcCardValidator.Result.Validated,
        block: suspend Scenario.() -> Unit,
    ) = runTest(dispatcher) {
        val fakeHardwareDelegate = FakeNfcHardwareDelegate(result = isHardwareAvailable)
        val fakeTransceiverFactory = FakeNfcTagTransceiverFactory(transceiver = transceiver)
        val fakeCardReader = FakeNfcCardReader(result = cardReadResult)
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
            fakeTransceiver = transceiver,
        ).block()

        fakeHardwareDelegate.ensureAllEventsConsumed()
        fakeTransceiverFactory.ensureAllEventsConsumed()
        fakeCardReader.ensureAllEventsConsumed()
        fakeCardValidator.ensureAllEventsConsumed()
        transceiver?.ensureAllEventsConsumed()
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
