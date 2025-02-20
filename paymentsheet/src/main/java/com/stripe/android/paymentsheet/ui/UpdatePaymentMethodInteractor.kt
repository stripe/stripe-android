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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

internal interface UpdatePaymentMethodInteractor {
    val topBarState: PaymentSheetTopBarState
    val canRemove: Boolean
    val displayableSavedPaymentMethod: DisplayableSavedPaymentMethod
    val screenTitle: ResolvableString?
    val cardBrandFilter: CardBrandFilter
    val isExpiredCard: Boolean
    val isModifiablePaymentMethod: Boolean
    val hasValidBrandChoices: Boolean
    val shouldShowSetAsDefaultCheckbox: Boolean

    val state: StateFlow<State>

    data class State(
        val error: ResolvableString?,
        val status: Status,
        val cardBrandChoice: CardBrandChoice,
        val cardBrandHasBeenChanged: Boolean,
        val setAsDefaultCheckboxChecked: Boolean,
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
        data class SetAsDefaultCheckboxChanged(val isChecked: Boolean) : ViewAction()
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
internal typealias UpdateCardBrandOperation = suspend (
    paymentMethod: PaymentMethod,
    brand: CardBrand
) -> Result<PaymentMethod>

internal class DefaultUpdatePaymentMethodInteractor(
    isLiveMode: Boolean,
    override val canRemove: Boolean,
    override val displayableSavedPaymentMethod: DisplayableSavedPaymentMethod,
    override val cardBrandFilter: CardBrandFilter,
    shouldShowSetAsDefaultCheckbox: Boolean,
    private val removeExecutor: PaymentMethodRemoveOperation,
    private val updateCardBrandExecutor: UpdateCardBrandOperation,
    private val onBrandChoiceOptionsShown: (CardBrand) -> Unit,
    private val onBrandChoiceOptionsDismissed: (CardBrand) -> Unit,
    workContext: CoroutineContext = Dispatchers.Default,
) : UpdatePaymentMethodInteractor {
    private val coroutineScope = CoroutineScope(workContext + SupervisorJob())
    private val error = MutableStateFlow(getInitialError())
    private val status = MutableStateFlow(UpdatePaymentMethodInteractor.Status.Idle)
    private val cardBrandChoice = MutableStateFlow(getInitialCardBrandChoice())
    private val cardBrandHasBeenChanged = MutableStateFlow(false)
    private val setAsDefaultCheckboxChecked = MutableStateFlow(false)
    private val savedCardBrand = MutableStateFlow(getInitialCardBrandChoice())

    // We don't yet support setting SEPA payment methods as defaults, so we hide the checkbox for now.
    override val shouldShowSetAsDefaultCheckbox = (
        shouldShowSetAsDefaultCheckbox &&
            displayableSavedPaymentMethod.savedPaymentMethod !is SavedPaymentMethod.SepaDebit
        )

    override val hasValidBrandChoices = hasValidBrandChoices()
    override val isExpiredCard = paymentMethodIsExpiredCard()
    override val screenTitle: ResolvableString? = UpdatePaymentMethodInteractor.screenTitle(
        displayableSavedPaymentMethod
    )
    override val isModifiablePaymentMethod: Boolean
        get() = !isExpiredCard && displayableSavedPaymentMethod.isModifiable()

    override val topBarState: PaymentSheetTopBarState = PaymentSheetTopBarStateFactory.create(
        isLiveMode = isLiveMode,
        editable = PaymentSheetTopBarState.Editable.Never,
    )

    private val _state = combineAsStateFlow(
        error,
        status,
        cardBrandChoice,
        cardBrandHasBeenChanged,
        setAsDefaultCheckboxChecked,
    ) { error, status, cardBrandChoice, cardBrandHasBeenChanged, setAsDefaultCheckboxChecked ->
        UpdatePaymentMethodInteractor.State(
            error = error,
            status = status,
            cardBrandChoice = cardBrandChoice,
            cardBrandHasBeenChanged = cardBrandHasBeenChanged,
            setAsDefaultCheckboxChecked = setAsDefaultCheckboxChecked,
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
            is UpdatePaymentMethodInteractor.ViewAction.SetAsDefaultCheckboxChanged -> onSetAsDefaultCheckboxChanged(
                isChecked = viewAction.isChecked
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
            error.emit(getInitialError())
            status.emit(UpdatePaymentMethodInteractor.Status.Updating)

            val updateCardBrandResult = maybeUpdateCardBrand()

            val errorMessage = getErrorMessageForUpdates(
                updateCardBrandResult = updateCardBrandResult,
            )

            if (errorMessage != null) {
                error.emit(errorMessage)
            }

            status.emit(UpdatePaymentMethodInteractor.Status.Idle)
        }
    }

    private suspend fun maybeUpdateCardBrand(): Result<PaymentMethod>? {
        val newCardBrand = cardBrandChoice.value.brand
        return if (newCardBrand != getInitialCardBrandChoice().brand) {
            updateCardBrandExecutor(
                displayableSavedPaymentMethod.paymentMethod,
                newCardBrand
            ).onSuccess {
                savedCardBrand.emit(CardBrandChoice(brand = newCardBrand, enabled = true))
                cardBrandHasBeenChanged.emit(false)
            }
        } else {
            null
        }
    }

    private fun getErrorMessageForUpdates(
        updateCardBrandResult: Result<PaymentMethod>?,
    ): ResolvableString? {
        return when {
            updateCardBrandResult?.isFailure == true -> updateCardBrandResult.exceptionOrNull()?.stripeErrorMessage()
            else -> null
        }
    }

    private fun onBrandChoiceChanged(cardBrandChoice: CardBrandChoice) {
        this.cardBrandChoice.value = cardBrandChoice
        this.cardBrandHasBeenChanged.value = cardBrandChoice != savedCardBrand.value

        onBrandChoiceOptionsDismissed(cardBrandChoice.brand)
    }

    private fun onSetAsDefaultCheckboxChanged(isChecked: Boolean) {
        setAsDefaultCheckboxChecked.update { isChecked }
    }

    private fun getInitialCardBrandChoice(): CardBrandChoice {
        return when (val savedPaymentMethod = displayableSavedPaymentMethod.savedPaymentMethod) {
            is SavedPaymentMethod.Card -> savedPaymentMethod.card.getPreferredChoice(cardBrandFilter)
            else -> CardBrandChoice(brand = CardBrand.Unknown, enabled = true)
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

    private fun hasValidBrandChoices(): Boolean {
        val filteredCardBrands = displayableSavedPaymentMethod.paymentMethod.card?.networks?.available?.map {
            CardBrand.fromCode(it)
        }?.filter { cardBrandFilter.isAccepted(it) }
        return (filteredCardBrands?.size ?: 0) > 1
    }
}

internal const val PaymentMethodRemovalDelayMillis = 600L
