package com.stripe.android.paymentsheet.ui

import com.stripe.android.CardBrandFilter
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.SavedPaymentMethod
import com.stripe.android.uicore.utils.combineAsStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

internal interface UpdatePaymentMethodInteractor {
    val isLiveMode: Boolean
    val canRemove: Boolean
    val displayableSavedPaymentMethod: DisplayableSavedPaymentMethod
    val screenTitle: ResolvableString?
    val cardBrandFilter: CardBrandFilter

    val state: StateFlow<State>

    data class State(
        val error: ResolvableString?,
        val isRemoving: Boolean,
        val cardBrandChoice: CardBrandChoice,
    )

    fun handleViewAction(viewAction: ViewAction)

    sealed class ViewAction {
        data object RemovePaymentMethod : ViewAction()
        data object BrandChoiceOptionsShown : ViewAction()
        data class BrandChoiceChanged(val cardBrandChoice: CardBrandChoice) : ViewAction()
        data object BrandChoiceOptionsDismissed : ViewAction()
    }

    companion object {
        fun screenTitle(displayableSavedPaymentMethod: DisplayableSavedPaymentMethod) = (
            when (displayableSavedPaymentMethod.savedPaymentMethod) {
                is SavedPaymentMethod.SepaDebit -> R.string.stripe_paymentsheet_manage_sepa_debit
                is SavedPaymentMethod.USBankAccount -> R.string.stripe_paymentsheet_manage_bank_account
                is SavedPaymentMethod.Card -> R.string.stripe_paymentsheet_manage_card
                SavedPaymentMethod.Unexpected -> null
            }
            )?.resolvableString
    }
}

internal class DefaultUpdatePaymentMethodInteractor(
    override val isLiveMode: Boolean,
    override val canRemove: Boolean,
    override val displayableSavedPaymentMethod: DisplayableSavedPaymentMethod,
    override val cardBrandFilter: CardBrandFilter,
    private val removeExecutor: PaymentMethodRemoveOperation,
    private val onBrandChoiceOptionsShown: (CardBrand) -> Unit,
    private val onBrandChoiceOptionsDismissed: (CardBrand) -> Unit,
    workContext: CoroutineContext = Dispatchers.Default,
) : UpdatePaymentMethodInteractor {
    private val coroutineScope = CoroutineScope(workContext + SupervisorJob())
    private val error = MutableStateFlow<ResolvableString?>(null)
    private val isRemoving = MutableStateFlow(false)
    private val cardBrandChoice = MutableStateFlow(getInitialCardBrandChoice())

    override val screenTitle: ResolvableString? = UpdatePaymentMethodInteractor.screenTitle(
        displayableSavedPaymentMethod
    )

    private val _state = combineAsStateFlow(
        error,
        isRemoving,
        cardBrandChoice,
    ) { error, isRemoving, cardBrandChoice ->
        UpdatePaymentMethodInteractor.State(
            error = error,
            isRemoving = isRemoving,
            cardBrandChoice = cardBrandChoice
        )
    }
    override val state = _state

    override fun handleViewAction(viewAction: UpdatePaymentMethodInteractor.ViewAction) {
        when (viewAction) {
            UpdatePaymentMethodInteractor.ViewAction.RemovePaymentMethod -> removePaymentMethod()
            is UpdatePaymentMethodInteractor.ViewAction.BrandChoiceOptionsShown -> onBrandChoiceOptionsShown(
                cardBrandChoice.value.brand
            )
            is UpdatePaymentMethodInteractor.ViewAction.BrandChoiceOptionsDismissed -> onBrandChoiceOptionsDismissed(
                cardBrandChoice.value.brand
            )
            is UpdatePaymentMethodInteractor.ViewAction.BrandChoiceChanged -> onBrandChoiceChanged(viewAction.cardBrandChoice)
        }
    }

    private fun removePaymentMethod() {
        coroutineScope.launch {
            error.emit(null)
            isRemoving.emit(true)

            val removeError = removeExecutor(displayableSavedPaymentMethod.paymentMethod)

            isRemoving.emit(false)
            error.emit(removeError?.stripeErrorMessage())
        }
    }

    private fun onBrandChoiceChanged(cardBrandChoice: CardBrandChoice) {
        this.cardBrandChoice.value = cardBrandChoice

        onBrandChoiceOptionsDismissed(cardBrandChoice.brand)
    }

    private fun getInitialCardBrandChoice(): CardBrandChoice {
        return when (val savedPaymentMethod = displayableSavedPaymentMethod.savedPaymentMethod) {
            is SavedPaymentMethod.Card -> savedPaymentMethod.card.getPreferredChoice()
            else -> CardBrandChoice(brand = CardBrand.Unknown)
        }
    }
}
