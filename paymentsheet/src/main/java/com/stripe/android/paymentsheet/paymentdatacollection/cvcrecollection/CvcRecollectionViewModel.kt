package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.ui.core.elements.CvcController
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class CvcRecollectionViewModel(args: Args) : ViewModel() {
    val controller = CvcController(cardBrandFlow = stateFlowOf(args.cardBrand))

    private val _viewState = MutableStateFlow(
        CvcRecollectionViewState(
            lastFour = args.lastFour,
            isTestMode = args.isTestMode,
            cvcState = CvcState(
                cvc = args.cvc,
                cardBrand = args.cardBrand
            ),
            isEnabled = true,
        )
    )
    val viewState: StateFlow<CvcRecollectionViewState>
        get() = _viewState

    private val _result = MutableSharedFlow<CvcRecollectionResult>()
    val result: SharedFlow<CvcRecollectionResult> = _result.asSharedFlow()

    fun handleViewAction(action: CvcRecollectionViewAction) {
        when (action) {
            is CvcRecollectionViewAction.OnConfirmPressed -> onConfirmPress(viewState.value.cvcState.cvc)
            is CvcRecollectionViewAction.OnBackPressed -> onBackPress()
            is CvcRecollectionViewAction.OnCvcChanged -> onCvcChanged(action.cvc)
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

    private fun onCvcChanged(cvc: String) {
        _viewState.update { oldState ->
            oldState.copy(
                cvcState = oldState.cvcState.updateCvc(cvc)
            )
        }
    }

    class Factory(
        private val args: CvcRecollectionContract.Args,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return CvcRecollectionViewModel(
                args = Args(
                    lastFour = args.lastFour,
                    cardBrand = args.cardBrand,
                    cvc = "",
                    isTestMode = args.isTestMode,
                )
            ) as T
        }
    }
}
