package com.stripe.android.paymentsheet.ui

import com.stripe.android.CardBrandFilter
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
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
    val isExpiredCard: Boolean
    val isModifiablePaymentMethod: Boolean

    val state: StateFlow<State>

    data class State(
        val error: ResolvableString?,
        val status: Status,
        val cardBrandChoice: CardBrandChoice,
        val cardBrandHasBeenChanged: Boolean,
    )

    enum class Status {
        Idle,
        Updating,
        Removing
    }

    fun handleViewAction(viewAction: ViewAction)

    sealed class ViewAction {
        data object RemovePaymentMethod : ViewAction()
        data object BrandChoiceOptionsShown : ViewAction()
        data class BrandChoiceChanged(val cardBrandChoice: CardBrandChoice) : ViewAction()
        data object BrandChoiceOptionsDismissed : ViewAction()
        data object SaveButtonPressed : ViewAction()
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

        val expiredErrorMessage: ResolvableString = com.stripe.android.R.string.stripe_expired_card.resolvableString
    }
}

internal typealias PaymentMethodRemoveOperation = suspend (paymentMethod: PaymentMethod) -> Throwable?
internal typealias PaymentMethodUpdateOperation = suspend (
    paymentMethod: PaymentMethod,
    brand: CardBrand
) -> Result<PaymentMethod>

internal class DefaultUpdatePaymentMethodInteractor(
    override val isLiveMode: Boolean,
    override val canRemove: Boolean,
    override val displayableSavedPaymentMethod: DisplayableSavedPaymentMethod,
    override val cardBrandFilter: CardBrandFilter,
    private val removeExecutor: PaymentMethodRemoveOperation,
    private val updateExecutor: PaymentMethodUpdateOperation,
    private val onBrandChoiceOptionsShown: (CardBrand) -> Unit,
    private val onBrandChoiceOptionsDismissed: (CardBrand) -> Unit,
    workContext: CoroutineContext = Dispatchers.Default,
) : UpdatePaymentMethodInteractor {
    private val coroutineScope = CoroutineScope(workContext + SupervisorJob())
    private val error = MutableStateFlow(getInitialError())
    private val status = MutableStateFlow(UpdatePaymentMethodInteractor.Status.Idle)
    private val cardBrandChoice = MutableStateFlow(getInitialCardBrandChoice())
    private val cardBrandHasBeenChanged = MutableStateFlow(false)
    private val savedCardBrand = MutableStateFlow(getInitialCardBrandChoice())

    override val isExpiredCard = paymentMethodIsExpiredCard()
    override val screenTitle: ResolvableString? = UpdatePaymentMethodInteractor.screenTitle(
        displayableSavedPaymentMethod
    )
    override val isModifiablePaymentMethod: Boolean
        get() = !isExpiredCard && displayableSavedPaymentMethod.isModifiable()

    private val _state = combineAsStateFlow(
        error,
        status,
        cardBrandChoice,
        cardBrandHasBeenChanged,
    ) { error, status, cardBrandChoice, cardBrandHasBeenChanged, ->
        UpdatePaymentMethodInteractor.State(
            error = error,
            status = status,
            cardBrandChoice = cardBrandChoice,
            cardBrandHasBeenChanged = cardBrandHasBeenChanged,
        )
    }
    override val state = _state

    override fun handleViewAction(viewAction: UpdatePaymentMethodInteractor.ViewAction) {
        when (viewAction) {
            UpdatePaymentMethodInteractor.ViewAction.RemovePaymentMethod -> removePaymentMethod()
            UpdatePaymentMethodInteractor.ViewAction.BrandChoiceOptionsShown -> onBrandChoiceOptionsShown(
                cardBrandChoice.value.brand
            )
            UpdatePaymentMethodInteractor.ViewAction.BrandChoiceOptionsDismissed -> onBrandChoiceOptionsDismissed(
                cardBrandChoice.value.brand
            )
            UpdatePaymentMethodInteractor.ViewAction.SaveButtonPressed -> savePaymentMethod()
            is UpdatePaymentMethodInteractor.ViewAction.BrandChoiceChanged -> onBrandChoiceChanged(
                viewAction.cardBrandChoice
            )
        }
    }

    private fun removePaymentMethod() {
        coroutineScope.launch {
            error.emit(getInitialError())
            status.emit(UpdatePaymentMethodInteractor.Status.Removing)

            val removeError = removeExecutor(displayableSavedPaymentMethod.paymentMethod)

            status.emit(UpdatePaymentMethodInteractor.Status.Idle)
            error.emit(removeError?.stripeErrorMessage() ?: getInitialError())
        }
    }

    private fun savePaymentMethod() {
        coroutineScope.launch {
            val newCardBrand = cardBrandChoice.value.brand

            error.emit(getInitialError())
            status.emit(UpdatePaymentMethodInteractor.Status.Updating)

            val updateResult = updateExecutor(displayableSavedPaymentMethod.paymentMethod, newCardBrand)

            updateResult.onSuccess {
                savedCardBrand.emit(CardBrandChoice(brand = newCardBrand))
                cardBrandHasBeenChanged.emit(false)
            }.onFailure {
                error.emit(it.stripeErrorMessage())
            }
            status.emit(UpdatePaymentMethodInteractor.Status.Idle)
        }
    }

    private fun onBrandChoiceChanged(cardBrandChoice: CardBrandChoice) {
        this.cardBrandChoice.value = cardBrandChoice
        this.cardBrandHasBeenChanged.value = cardBrandChoice != savedCardBrand.value

        onBrandChoiceOptionsDismissed(cardBrandChoice.brand)
    }

    private fun getInitialCardBrandChoice(): CardBrandChoice {
        return when (val savedPaymentMethod = displayableSavedPaymentMethod.savedPaymentMethod) {
            is SavedPaymentMethod.Card -> savedPaymentMethod.card.getPreferredChoice()
            else -> CardBrandChoice(brand = CardBrand.Unknown)
        }
    }

    private fun paymentMethodIsExpiredCard(): Boolean {
        return (displayableSavedPaymentMethod.savedPaymentMethod as? SavedPaymentMethod.Card)?.isExpired() ?: false
    }

    private fun getInitialError(): ResolvableString? {
        return if (paymentMethodIsExpiredCard()) {
            UpdatePaymentMethodInteractor.expiredErrorMessage
        } else {
            null
        }
    }
}

internal const val PaymentMethodRemovalDelayMillis = 600L
