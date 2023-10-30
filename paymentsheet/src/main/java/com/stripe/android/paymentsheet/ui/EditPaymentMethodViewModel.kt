package com.stripe.android.paymentsheet.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class EditPaymentMethodViewModel(
    private val paymentMethod: PaymentMethod
) : ViewModel() {
    private val card = paymentMethod.card

    private val last4 = card?.last4 ?: throw IllegalStateException("Requires Last 4")

    private val initialChoice = card?.networks?.preferred?.let { preferredCode ->
        CardBrand.fromCode(preferredCode).toChoice()
    } ?: CardBrand.Unknown.toChoice()

    private val availableChoices = card?.networks?.available?.let { brandCodes ->
        brandCodes.map { code ->
            CardBrand.fromCode(code).toChoice()
        }
    } ?: throw IllegalStateException("Requires choices")

    private val currentChoice = MutableStateFlow(initialChoice)

    private val _effect = MutableSharedFlow<EditPaymentMethodViewEffect>()
    val effect: SharedFlow<EditPaymentMethodViewEffect> = _effect.asSharedFlow()

    private val _viewState = MutableStateFlow(
        EditPaymentViewState(
            last4 = last4,
            selectedBrand = initialChoice,
            canUpdate = false,
            availableBrands = availableChoices
        )
    )
    val viewState: StateFlow<EditPaymentViewState> = _viewState.asStateFlow()

    init {
        viewModelScope.launch {
            currentChoice.collect { choice ->
                _viewState.value = EditPaymentViewState(
                    last4 = last4,
                    canUpdate = initialChoice != choice,
                    selectedBrand = choice,
                    availableBrands = availableChoices
                )
            }
        }
    }

    fun handleViewAction(action: EditPaymentMethodViewAction) {
        when (action) {
            is EditPaymentMethodViewAction.OnRemovePressed -> onRemovePressed()
            is EditPaymentMethodViewAction.OnUpdatePressed -> onUpdatePressed()
            is EditPaymentMethodViewAction.OnBrandChoiceChanged -> {
                onBrandChoiceChanged(action.choice)
            }
        }
    }

    private fun onRemovePressed() {
        viewModelScope.launch {
            _effect.emit(EditPaymentMethodViewEffect.OnRemoveRequested(paymentMethod))
        }
    }

    private fun onUpdatePressed() {
        if (_viewState.value.canUpdate) {
            viewModelScope.launch {
                _effect.emit(
                    EditPaymentMethodViewEffect.OnUpdateRequested(
                        paymentMethod = paymentMethod,
                        brand = currentChoice.value.brand
                    )
                )
            }
        }
    }

    private fun onBrandChoiceChanged(choice: EditPaymentViewState.CardBrandChoice) {
        currentChoice.value = choice
    }

    private fun CardBrand.toChoice(): EditPaymentViewState.CardBrandChoice {
        return EditPaymentViewState.CardBrandChoice(brand = this)
    }

    class Factory(
        private val paymentMethod: PaymentMethod
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            @Suppress("UNCHECKED_CAST")
            return EditPaymentMethodViewModel(paymentMethod = paymentMethod) as T
        }
    }
}
