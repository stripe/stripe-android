package com.stripe.android.common.nfcscan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.common.nfcscan.tapzone.TapZoneResolver
import com.stripe.android.core.utils.requireApplication
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class NfcScanningViewModel @Inject constructor(
    tapZoneResolver: TapZoneResolver,
) : ViewModel() {
    private val tapZone = tapZoneResolver.get()

    private val _result = MutableSharedFlow<NfcScanningContract.Result>()
    val result = _result.asSharedFlow()

    val viewState: StateFlow<NfcScanningViewState> = stateFlowOf(NfcScanningViewState(tapZone))

    fun handleViewAction(viewAction: NfcScanningViewAction) {
        when (viewAction) {
            is NfcScanningViewAction.Close -> {
                viewModelScope.launch {
                    _result.emit(NfcScanningContract.Result.Canceled)
                }
            }
        }
    }

    companion object {
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
