package com.stripe.android.common.nfcscan

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.common.nfcscan.scanner.NfcCardScanner
import com.stripe.android.common.nfcscan.tapzone.TapZoneResolver
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.utils.requireApplication
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class NfcScanningViewModel @Inject constructor(
    @ViewModelScope private val viewModelScope: CoroutineScope,
    tapZoneResolver: TapZoneResolver,
    private val cardScanner: NfcCardScanner,
) : ViewModel() {
    private val tapZone = tapZoneResolver.get()

    private val _result = MutableSharedFlow<NfcScanningContract.Result>()
    val result = _result.asSharedFlow()

    val viewState: StateFlow<NfcScanningViewState> = stateFlowOf(NfcScanningViewState(tapZone))

    init {
        viewModelScope.launch {
            cardScanner.state.collectLatest { state ->
                when (state) {
                    is NfcCardScanner.State.Scanning,
                    is NfcCardScanner.State.Failed -> Unit
                    is NfcCardScanner.State.Complete -> {
                        _result.emit(
                            NfcScanningContract.Result.Complete(
                                cardNumber = state.cardData.cardNumber,
                                expirationMonth = state.cardData.expirationMonth,
                                expirationYear = state.cardData.expirationYear,
                            )
                        )
                    }
                }
            }
        }
    }

    fun register(activity: AppCompatActivity) {
        cardScanner.start(activity)
    }

    fun handleViewAction(viewAction: NfcScanningViewAction) {
        when (viewAction) {
            is NfcScanningViewAction.Close -> {
                viewModelScope.launch {
                    _result.emit(NfcScanningContract.Result.Canceled)
                }
            }
        }
    }

    override fun onCleared() {
        viewModelScope.cancel()
        super.onCleared()
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
