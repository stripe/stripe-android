package com.stripe.android.common.nfcscan.scanner

import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.common.nfcscan.hardware.NfcHardwareDelegate
import com.stripe.android.common.nfcscan.security.IsDeveloperOptionsEnabled
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentsheet.R
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
        val userMessage: ResolvableString,
    )

    sealed interface EnablementState {
        val enabled: Boolean

        data object Enabled : EnablementState {
            override val enabled = true
        }

        data class Disabled(val message: ResolvableString) : EnablementState {
            override val enabled = false
        }
    }

    val state: Flow<State>
    val enablementState: EnablementState

    fun start(activity: AppCompatActivity)
}

internal class DefaultNfcCardScanner @Inject constructor(
    private val hardwareDelegate: NfcHardwareDelegate,
    private val isDeveloperOptionsEnabled: IsDeveloperOptionsEnabled,
    private val cardReader: NfcCardReader,
    private val transceiverFactory: NfcTagTransceiver.Factory,
    @ViewModelScope private val viewModelScope: CoroutineScope,
    @IOContext private val workContext: CoroutineContext,
) : NfcCardScanner {
    private val _state = MutableSharedFlow<NfcCardScanner.State>()
    override val state: Flow<NfcCardScanner.State> = _state.asSharedFlow()

    override val enablementState: NfcCardScanner.EnablementState by lazy {
        resolveEnablementState()
    }

    override fun start(
        activity: AppCompatActivity,
    ) {
        if (!enablementState.enabled) {
            return
        }

        hardwareDelegate.start(
            activity = activity,
        ) { tag ->
            val transceiver = transceiverFactory.create(tag)
                ?: return@start

            viewModelScope.launch(workContext) {
                _state.emit(NfcCardScanner.State.Scanning)

                runCatching {
                    transceiver.open()

                    try {
                        cardReader.readCard(transceiver)
                            .getOrThrow()
                    } finally {
                        transceiver.close()
                    }
                }.fold(
                    onSuccess = { cardData ->
                        _state.emit(NfcCardScanner.State.Complete(cardData))
                    },
                    onFailure = {
                        _state.emit(NfcCardScanner.State.Failed(GENERIC_ERROR))
                    }
                )
            }
        }
    }

    private fun resolveEnablementState(): NfcCardScanner.EnablementState {
        if (!hardwareDelegate.isAvailable()) {
            return NfcCardScanner.EnablementState.Disabled(
                R.string.stripe_nfc_scan_full_screen_error_reader.resolvableString,
            )
        }

        if (!isDeveloperOptionsEnabled.get()) {
            return NfcCardScanner.EnablementState.Disabled(
                R.string.stripe_nfc_scan_full_screen_error_dev_options_action.resolvableString,
            )
        }

        return NfcCardScanner.EnablementState.Enabled
    }

    private companion object {
        val GENERIC_ERROR = NfcCardScanner.Error(
            userMessage = R.string.stripe_tap_to_add_card_default_error_action.resolvableString,
        )
    }
}
