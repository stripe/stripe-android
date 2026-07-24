package com.stripe.android.common.nfcscan.scanner

import android.nfc.Tag
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.common.nfcscan.NfcScanLogger
import com.stripe.android.common.nfcscan.hardware.NfcHardwareDelegate
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.strings.ResolvableString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

internal interface NfcCardScanner {
    sealed interface State {
        data object Scanning : State
        data class Complete(val cardData: ScannedCardData) : State
        data class Failed(val error: Error) : State
    }

    data class Error(
        val code: String,
        val userMessage: ResolvableString,
    )

    val state: Flow<State>

    fun start(activity: AppCompatActivity)
}

internal class DefaultNfcCardScanner @Inject constructor(
    private val hardwareDelegate: NfcHardwareDelegate,
    private val cardReader: NfcCardReader,
    private val cardValidator: NfcCardValidator,
    private val transceiverFactory: NfcTagTransceiver.Factory,
    @ViewModelScope private val viewModelScope: CoroutineScope,
    @IOContext private val workContext: CoroutineContext,
) : NfcCardScanner {
    private val _state = MutableSharedFlow<NfcCardScanner.State>()
    override val state: Flow<NfcCardScanner.State> = _state.asSharedFlow()

    override fun start(
        activity: AppCompatActivity,
    ) {
        NfcScanLogger.debug("Starting NFC card scanner")
        hardwareDelegate.start(
            activity = activity,
        ) { tag ->
            val transceiver = transceiverFor(tag) ?: return@start

            viewModelScope.launch(workContext) {
                scanCard(transceiver)
            }
        }
    }

    private fun transceiverFor(tag: Tag): NfcTagTransceiver? {
        val techList = runCatching {
            tag.techList.toList()
        }.getOrElse {
            emptyList()
        }
        NfcScanLogger.debug("Tag discovered techs=$techList")

        return transceiverFactory.create(tag)
            ?: run {
                NfcScanLogger.debug("No IsoDep transceiver available for tag")
                null
            }
    }

    private suspend fun scanCard(transceiver: NfcTagTransceiver) {
        _state.emit(NfcCardScanner.State.Scanning)
        NfcScanLogger.debug("Scanner state emitted: Scanning")

        val cardData = readCardData(transceiver) ?: return
        val finalResult = validate(cardData)

        NfcScanLogger.debug(finalResult.toLogMessage())
        _state.emit(finalResult)
    }

    private suspend fun readCardData(transceiver: NfcTagTransceiver): ScannedCardData? {
        return when (val readerResult = cardReader.readCard(transceiver)) {
            is NfcCardReader.Result.Found -> {
                NfcScanLogger.debug(
                    "Reader found card brand digits=${readerResult.scannedCardData.cardNumber.length} " +
                        "expiry=${readerResult.scannedCardData.expirationMonth}/" +
                        readerResult.scannedCardData.expirationYear
                )
                readerResult.scannedCardData
            }
            is NfcCardReader.Result.Error -> {
                NfcScanLogger.debug("Reader failed code=${readerResult.errorCode}")
                _state.emit(
                    NfcCardScanner.State.Failed(
                        error = NfcCardScanner.Error(
                            code = readerResult.errorCode,
                            userMessage = readerResult.userMessage,
                        )
                    )
                )
                null
            }
        }
    }

    private fun validate(cardData: ScannedCardData): NfcCardScanner.State {
        return when (val result = cardValidator.validate(cardData)) {
            is NfcCardValidator.Result.Validated -> {
                NfcScanLogger.debug("Card validation succeeded")
                NfcCardScanner.State.Complete(cardData)
            }
            is NfcCardValidator.Result.Invalid -> {
                NfcScanLogger.debug("Card validation failed code=${result.errorCode}")
                NfcCardScanner.State.Failed(
                    error = NfcCardScanner.Error(
                        code = result.errorCode,
                        userMessage = result.userMessage,
                    )
                )
            }
        }
    }

    private fun NfcCardScanner.State.toLogMessage(): String {
        return when (this) {
            is NfcCardScanner.State.Complete -> "Scanner final state=Complete"
            is NfcCardScanner.State.Failed -> "Scanner final state=Failed code=${error.code}"
            NfcCardScanner.State.Scanning -> "Scanner final state=Scanning"
        }
    }
}
