package com.stripe.android.common.nfcscan

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.common.nfcscan.injection.DaggerNfcScanningViewModelComponent
import com.stripe.android.common.nfcscan.tapzone.TapZone
import com.stripe.android.common.nfcscan.tapzone.TapZoneResolver
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.utils.requireApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class NfcScanningViewModel @Inject constructor(
    private val scanner: NfcCardScanner,
    tapZoneResolver: TapZoneResolver,
    @ViewModelScope private val viewModelScope: CoroutineScope
) : ViewModel() {

    private val _state = MutableStateFlow<NfcScanningState>(NfcScanningState.Scanning)
    val state: StateFlow<NfcScanningState> = _state.asStateFlow()

    private val _shouldSave = MutableStateFlow(true)
    val shouldSave: StateFlow<Boolean> = _shouldSave.asStateFlow()

    val tapZone: TapZone = tapZoneResolver.get()

    init {
        viewModelScope.launch {
            scanner.state.collect { scanState ->
                when (scanState) {
                    is NfcCardScanner.State.Scanning -> {
                        _state.value = NfcScanningState.Reading
                    }
                    is NfcCardScanner.State.Complete -> {
                        _state.value = NfcScanningState.Complete(scanState.cardData)
                    }
                    is NfcCardScanner.State.Failed -> {
                        _state.value = NfcScanningState.Failed(scanState.error)
                        launch {
                            delay(FAILED_RESET_DELAY_MS)
                            if (_state.value is NfcScanningState.Failed) {
                                _state.value = NfcScanningState.Scanning
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.cancel()
    }

    fun register(activity: AppCompatActivity) {
        scanner.scan(activity)
    }

    fun onShouldSaveChanged(shouldSave: Boolean) {
        _shouldSave.value = shouldSave
    }

    companion object {
        private const val FAILED_RESET_DELAY_MS = 2500L

        fun factory(): ViewModelProvider.Factory {
            return viewModelFactory {
                initializer<NfcScanningViewModel> {
                    DaggerNfcScanningViewModelComponent.factory()
                        .create(requireApplication())
                        .viewModel
                }
            }
        }
    }
}
