package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

internal class CvcRecollectionViewModel(
    private val interactor: CvcRecollectionInteractor
) : ViewModel(), CvcRecollectionInteractor by interactor {
    private val _result = MutableSharedFlow<CvcRecollectionResult>()

    // This is used in DisplayMode.Activity to send confirmation or cancellation events
    val result: SharedFlow<CvcRecollectionResult> = _result.asSharedFlow()

    override fun handleViewAction(action: CvcRecollectionViewAction) {
        when (action) {
            is CvcRecollectionViewAction.OnConfirmPressed -> onConfirmPress(action.cvc)
            is CvcRecollectionViewAction.OnBackPressed -> onBackPress()
            else -> interactor.handleViewAction(action)
        }
    }

    private fun onConfirmPress(cvc: String) {
        viewModelScope.launch {
            _result.emit(CvcRecollectionResult.Confirmed(cvc))
        }
    }

    private fun onBackPress() {
        viewModelScope.launch {
            _result.emit(CvcRecollectionResult.Cancelled)
        }
    }

    class Factory(
        private val args: CvcRecollectionContract.Args,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return CvcRecollectionViewModel(
                interactor = DefaultCvcRecollectionInteractor(
                    args = Args(
                        lastFour = args.lastFour,
                        cardBrand = args.cardBrand,
                        cvc = null,
                        isTestMode = args.isTestMode
                    )
                )
            ) as T
        }
    }
}
