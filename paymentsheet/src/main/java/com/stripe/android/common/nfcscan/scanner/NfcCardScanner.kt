package com.stripe.android.common.nfcscan.scanner

import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.common.nfcscan.hardware.NfcHardwareDelegate
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.ViewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

internal interface NfcCardScanner {
    sealed interface State {
        data object Scanning : State
        data class Complete(val cardData: ScannedCardData) : State
        data class Failed(val error: Throwable) : State
    }

    val state: Flow<State>

    fun start(activity: AppCompatActivity)
}

internal class DefaultNfcCardScanner @Inject constructor(
    private val hardwareDelegate: NfcHardwareDelegate,
    private val cardReader: NfcCardReader,
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

                try {
                    transceiver.open()
                } catch (e: SecurityException) {
                    _state.emit(NfcCardScanner.State.Failed(e))
                    return@launch
                } catch (e: IOException) {
                    _state.emit(NfcCardScanner.State.Failed(e))
                    return@launch
                }

                cardReader.readCard(transceiver).fold(
                    onSuccess = { cardData ->
                        _state.emit(NfcCardScanner.State.Complete(cardData))
                    },
                    onFailure = {
                        _state.emit(NfcCardScanner.State.Failed(it))
                    },
                )

                try {
                    transceiver.close()
                } catch (_: SecurityException) {
                    // ignore close failures
                } catch (_: IOException) {
                    // ignore close failures
                }
            }
        }
    }
}
