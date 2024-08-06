package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.model.CardBrand
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class CvcRecollectionViewModel(args: Args) : ViewModel() {
    private val _result = MutableSharedFlow<CvcRecollectionResult>()
    val result: SharedFlow<CvcRecollectionResult> = _result.asSharedFlow()

    private val _viewState = MutableStateFlow(
        CvcRecollectionViewState(
            cardBrand = args.cardBrand,
            lastFour = args.lastFour,
            cvc = null,
            displayMode = args.displayMode
        )
    )
    val viewState: StateFlow<CvcRecollectionViewState> = _viewState.asStateFlow()

    fun handleViewAction(action: CvcRecollectionViewAction) {
        when (action) {
            is CvcRecollectionViewAction.OnConfirmPressed -> onConfirmPress(action.cvc)
            is CvcRecollectionViewAction.OnBackPressed -> onBackPress()
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

    data class Args(
        val lastFour: String,
        val cardBrand: CardBrand,
        val cvc: String? = null,
        val displayMode: DisplayMode
    ) {
        sealed interface DisplayMode {
            val isLiveMode: Boolean

            data class Activity(override val isLiveMode: Boolean) : DisplayMode
            data class PaymentScreen(override val isLiveMode: Boolean) : DisplayMode
        }
    }

    class Factory(private val args: CvcRecollectionContract.Args) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return CvcRecollectionViewModel(
                Args(
                    lastFour = args.lastFour,
                    cardBrand = args.cardBrand,
                    cvc = null,
                    displayMode = args.displayMode.toDisplayMode()
                )
            ) as T
        }
    }
}

internal fun CvcRecollectionContract.Args.DisplayMode.toDisplayMode(): CvcRecollectionViewModel.Args.DisplayMode {
    return when (this) {
        is CvcRecollectionContract.Args.DisplayMode.Activity -> {
            CvcRecollectionViewModel.Args.DisplayMode.Activity(isLiveMode)
        }
        is CvcRecollectionContract.Args.DisplayMode.PaymentScreen -> {
            CvcRecollectionViewModel.Args.DisplayMode.PaymentScreen(isLiveMode)
        }
    }
}
