package com.stripe.android.common.nfcscan

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.common.nfcscan.analytics.NfcScanCancellationReason
import com.stripe.android.common.nfcscan.analytics.NfcScanningEventReporter
import com.stripe.android.common.nfcscan.scanner.NfcCardScanner
import com.stripe.android.common.nfcscan.scanner.ScannedCardData
import com.stripe.android.common.nfcscan.tapzone.TapZoneResolver
import com.stripe.android.common.nfcscan.ui.HapticFeedbackType
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
    private val timeoutManager: NfcScanningTimeoutManager,
    private val eventReporter: NfcScanningEventReporter,
) : ViewModel() {
    private val tapZone = tapZoneResolver.get()

    private val _event = MutableSharedFlow<NfcScanningEvent>()
    val event = _event.asSharedFlow()

    private val _viewState = MutableStateFlow(
        NfcScanningViewState(
            tapZone = tapZone,
            status = NfcScanningStatus.Idle(error = null),
        )
    )
    val viewState: StateFlow<NfcScanningViewState> = _viewState.asStateFlow()

    private var pendingValidCardData: ScannedCardData? = null
    private var successShownDispatched = false
    private var isFlowClosed = false

    init {
        eventReporter.onNfcScanStarted()
        timeoutManager.start()

        viewModelScope.launch {
            cardScanner.state.collectLatest { state ->
                when (state) {
                    is NfcCardScanner.State.Scanning -> {
                        timeoutManager.reset()

                        _viewState.emit(
                            NfcScanningViewState(
                                tapZone = tapZone,
                                status = NfcScanningStatus.Scanning,
                            )
                        )

                        eventReporter.onNfcScanAttemptStarted()
                    }
                    is NfcCardScanner.State.Failed -> {
                        timeoutManager.reset()

                        _event.emit(NfcScanningEvent.TriggerHapticFeedback(HapticFeedbackType.Failed))

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
                        timeoutManager.cancel()

                        _event.emit(NfcScanningEvent.TriggerHapticFeedback(HapticFeedbackType.Success))

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

        viewModelScope.launch {
            timeoutManager.timeout.collectLatest {
                if (pendingValidCardData == null) {
                    cancel(NfcScanCancellationReason.Timeout)
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
                    cancel(NfcScanCancellationReason.UserInitiated)
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
                        _event.emit(
                            NfcScanningEvent.CloseWithResult(
                                NfcScanningContract.Result.Complete(
                                    cardNumber = cardData.cardNumber,
                                    expirationYear = cardData.expirationYear,
                                    expirationMonth = cardData.expirationMonth,
                                )
                            )
                        )
                    }
                }
            }
        }
    }

    override fun onCleared() {
        timeoutManager.cancel()
        viewModelScope.cancel()
        super.onCleared()
    }

    private suspend fun cancel(reason: NfcScanCancellationReason) {
        if (isFlowClosed) {
            return
        }

        isFlowClosed = true
        timeoutManager.cancel()
        eventReporter.onNfcScanCancelled(reason)
        _event.emit(NfcScanningEvent.CloseWithResult(NfcScanningContract.Result.Canceled))
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
