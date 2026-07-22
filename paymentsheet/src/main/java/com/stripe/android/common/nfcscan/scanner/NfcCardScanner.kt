package com.stripe.android.common.nfcscan.scanner

import androidx.appcompat.app.AppCompatActivity
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
        hardwareDelegate.start(
            activity = activity,
        ) { tag ->
            val transceiver = transceiverFactory.create(tag)
                ?: return@start

            viewModelScope.launch(workContext) {
                _state.emit(NfcCardScanner.State.Scanning)

                val cardData = when (val readerResult = cardReader.readCard(transceiver)) {
                    is NfcCardReader.Result.Found -> readerResult.scannedCardData
                    is NfcCardReader.Result.Error -> {
                        _state.emit(
                            NfcCardScanner.State.Failed(
                                error = NfcCardScanner.Error(
                                    code = readerResult.errorCode,
                                    userMessage = readerResult.userMessage,
                                )
                            )
                        )

                        return@launch
                    }
                }

                val finalResult = when (val result = cardValidator.validate(cardData)) {
                    is NfcCardValidator.Result.Validated -> NfcCardScanner.State.Complete(cardData)
                    is NfcCardValidator.Result.Invalid -> NfcCardScanner.State.Failed(
                        error = NfcCardScanner.Error(
                            code = result.errorCode,
                            userMessage = result.userMessage,
                        )
                    )
                }

                _state.emit(finalResult)
            }
        }
    }
}
