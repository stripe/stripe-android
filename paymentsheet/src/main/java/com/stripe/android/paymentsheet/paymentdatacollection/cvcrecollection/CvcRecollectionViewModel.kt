package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.ui.core.elements.CvcController
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

internal class CvcRecollectionViewModel(args: Args) : ViewModel() {
    val controller = CvcController(cardBrandFlow = stateFlowOf(args.cardBrand))

    private val _viewState = MutableStateFlow(
        CvcRecollectionViewState(
            cardBrand = args.cardBrand,
            lastFour = args.lastFour,
            cvc = null,
            controller = controller,
            displayMode = args.displayMode
        )
    )
    val viewState: StateFlow<CvcRecollectionViewState>
        get() = _viewState

    private val _result = MutableSharedFlow<CvcRecollectionResult>()
    val result: SharedFlow<CvcRecollectionResult> = _result.asSharedFlow()

    fun handleViewAction(action: CvcRecollectionViewAction) {
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
        private val onCompletionChanged: (CVCRecollectionCompletion) -> Unit,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return CvcRecollectionViewModel(
                args = Args(
                    lastFour = args.lastFour,
                    cardBrand = args.cardBrand,
                    cvc = null,
                    displayMode = args.displayMode.toDisplayMode()
                )
            ) as T
        }
    }
}

internal fun CvcRecollectionContract.Args.DisplayMode.toDisplayMode(): Args.DisplayMode {
    return when (this) {
        is CvcRecollectionContract.Args.DisplayMode.Activity -> {
            Args.DisplayMode.Activity(isLiveMode)
        }
        is CvcRecollectionContract.Args.DisplayMode.PaymentScreen -> {
            Args.DisplayMode.PaymentScreen(isLiveMode)
        }
    }
}
