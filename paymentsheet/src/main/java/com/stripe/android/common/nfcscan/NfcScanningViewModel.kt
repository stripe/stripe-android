package com.stripe.android.common.nfcscan

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.common.nfcscan.analytics.NfcScanningEventReporter
import com.stripe.android.common.nfcscan.scanner.NfcCardScanner
import com.stripe.android.common.nfcscan.scanner.ScannedCardData
import com.stripe.android.common.nfcscan.tapzone.TapZoneResolver
import com.stripe.android.common.nfcscan.ui.NfcScanningStatus
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.utils.requireApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class NfcScanningViewModel @Inject constructor(
    @ViewModelScope private val viewModelScope: CoroutineScope,
    tapZoneResolver: TapZoneResolver,
    private val cardScanner: NfcCardScanner,
    private val eventReporter: NfcScanningEventReporter,
) : ViewModel() {
    private val tapZone = tapZoneResolver.get()

    private val _result = MutableSharedFlow<NfcScanningContract.Result>()
    val result = _result.asSharedFlow()

    private val _viewState = MutableStateFlow(
        NfcScanningViewState(
            tapZone = tapZone,
            status = NfcScanningStatus.Idle(error = null),
        )
    )
    val viewState: StateFlow<NfcScanningViewState> = _viewState.asStateFlow()

    private var pendingValidCardData: ScannedCardData? = null
    private var successShownDispatched = false

    init {
        eventReporter.onNfcScanStarted()

        viewModelScope.launch {
            cardScanner.state.collectLatest { state ->
                when (state) {
                    is NfcCardScanner.State.Scanning -> {
                        _viewState.emit(
                            NfcScanningViewState(
                                tapZone = tapZone,
                                status = NfcScanningStatus.Scanning,
                            )
                        )

                        eventReporter.onNfcScanAttemptStarted()
                    }
                    is NfcCardScanner.State.Failed -> {
                        val error = state.error

                        _viewState.emit(
                            NfcScanningViewState(
                                tapZone = tapZone,
                                status = NfcScanningStatus.Idle(error = error.userMessage),
                            )
                        )

                        eventReporter.onNfcScanAttemptFailed(errorCode = error.code)
                    }
                    is NfcCardScanner.State.Complete -> {
                        _viewState.emit(
                            NfcScanningViewState(
                                tapZone = tapZone,
                                status = NfcScanningStatus.Scanned,
                            )
                        )

                        eventReporter.onNfcScanAttemptSucceeded()

                        pendingValidCardData = state.cardData
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
                    eventReporter.onNfcScanCancelled()
                    _result.emit(NfcScanningContract.Result.Canceled)
                }
            }
            is NfcScanningViewAction.SuccessShown -> {
                if (successShownDispatched) {
                    return
                }
                successShownDispatched = true
                eventReporter.onNfcScanSucceeded()
                viewModelScope.launch {
                    pendingValidCardData?.let { cardData ->
                        _result.emit(
                            NfcScanningContract.Result.Complete(
                                cardNumber = cardData.cardNumber,
                                expirationYear = cardData.expirationYear,
                                expirationMonth = cardData.expirationMonth,
                            )
                        )
                    }
                }
            }
        }
    }

    override fun onCleared() {
        viewModelScope.cancel()
        super.onCleared()
    }

    companion object {
        fun factory(argsSupplier: () -> NfcScanningContract.Args): ViewModelProvider.Factory {
            return viewModelFactory {
                initializer<NfcScanningViewModel> {
                    val args = argsSupplier()

                    DaggerNfcScanningViewModelComponent.factory()
                        .create(
                            application = requireApplication(),
                            paymentMethodMetadata = args.paymentMethodMetadata,
                        )
                        .viewModel
                }
            }
        }
    }
}
