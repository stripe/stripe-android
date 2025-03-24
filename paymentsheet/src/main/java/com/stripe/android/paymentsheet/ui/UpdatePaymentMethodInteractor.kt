package com.stripe.android.paymentsheet.ui

import androidx.annotation.VisibleForTesting
import com.stripe.android.CardBrandFilter
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.CardUpdateParams
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
    val setAsDefaultCheckboxEnabled: Boolean
    val shouldShowSaveButton: Boolean
    val allowCardEdit: Boolean

    val state: StateFlow<State>

    data class State(
        val error: ResolvableString?,
        val status: Status,
        val cardBrandChoice: CardBrandChoice,
        val setAsDefaultCheckboxChecked: Boolean,
        val isSaveButtonEnabled: Boolean,
    )

    enum class Status(val isPerformingNetworkOperation: Boolean) {
        Idle(isPerformingNetworkOperation = false),
        Updating(isPerformingNetworkOperation = true),
        Removing(isPerformingNetworkOperation = true)
    }

    fun handleViewAction(viewAction: ViewAction)

    sealed class ViewAction {
        data object RemovePaymentMethod : ViewAction()
        data class BrandChoiceChanged(val cardBrandChoice: CardBrandChoice) : ViewAction()
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
internal typealias UpdateCardPaymentMethodOperation = suspend (
    paymentMethod: PaymentMethod,
    cardUpdateParams: CardUpdateParams
) -> Result<PaymentMethod>
internal typealias PaymentMethodSetAsDefaultOperation = suspend (
    paymentMethod: PaymentMethod
) -> Result<Unit>

internal class DefaultUpdatePaymentMethodInteractor(
    isLiveMode: Boolean,
    override val canRemove: Boolean,
    override val displayableSavedPaymentMethod: DisplayableSavedPaymentMethod,
    override val cardBrandFilter: CardBrandFilter,
    val isDefaultPaymentMethod: Boolean,
    shouldShowSetAsDefaultCheckbox: Boolean,
    private val removeExecutor: PaymentMethodRemoveOperation,
    private val updatePaymentMethodExecutor: UpdateCardPaymentMethodOperation,
    private val setDefaultPaymentMethodExecutor: PaymentMethodSetAsDefaultOperation,
    private val onBrandChoiceSelected: (CardBrand) -> Unit,
    private val onUpdateSuccess: () -> Unit,
    workContext: CoroutineContext = Dispatchers.Default,
) : UpdatePaymentMethodInteractor {
    private val coroutineScope = CoroutineScope(workContext + SupervisorJob())
    private val error = MutableStateFlow(getInitialError())
    private val status = MutableStateFlow(UpdatePaymentMethodInteractor.Status.Idle)
    private val cardBrandChoice = MutableStateFlow(getInitialCardBrandChoice())
    private val cardBrandHasBeenChanged = MutableStateFlow(false)
    private val initialSetAsDefaultCheckedValue = isDefaultPaymentMethod
    private val setAsDefaultCheckboxChecked = MutableStateFlow(initialSetAsDefaultCheckedValue)
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
    override val setAsDefaultCheckboxEnabled: Boolean = !isDefaultPaymentMethod

    override val shouldShowSaveButton: Boolean = isModifiablePaymentMethod ||
        (shouldShowSetAsDefaultCheckbox && !isDefaultPaymentMethod)
    override val allowCardEdit = FeatureFlags.editSavedCardPaymentMethodEnabled.isEnabled

    private val _state = combineAsStateFlow(
        error,
        status,
        cardBrandChoice,
        cardBrandHasBeenChanged,
        setAsDefaultCheckboxChecked,
    ) { error, status, cardBrandChoice, cardBrandHasBeenChanged, setAsDefaultCheckboxChecked ->
        val setAsDefaultValueChanged = setAsDefaultCheckboxChecked != initialSetAsDefaultCheckedValue
        val isSaveButtonEnabled = (cardBrandHasBeenChanged || setAsDefaultValueChanged) &&
            status == UpdatePaymentMethodInteractor.Status.Idle

        UpdatePaymentMethodInteractor.State(
            error = error,
            status = status,
            cardBrandChoice = cardBrandChoice,
            isSaveButtonEnabled = isSaveButtonEnabled,
            setAsDefaultCheckboxChecked = isDefaultPaymentMethod || setAsDefaultCheckboxChecked,
        )
    }
    override val state = _state

    override fun handleViewAction(viewAction: UpdatePaymentMethodInteractor.ViewAction) {
        when (viewAction) {
            UpdatePaymentMethodInteractor.ViewAction.RemovePaymentMethod -> removePaymentMethod()
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
            val setDefaultPaymentMethodResult = maybeSetDefaultPaymentMethod()

            val updateResult = getUpdateResult(
                updateCardBrandResult = updateCardBrandResult,
                setDefaultPaymentMethodResult = setDefaultPaymentMethodResult,
            )

            when (updateResult) {
                is UpdateResult.Error -> error.emit(updateResult.errorMessage)
                UpdateResult.Success -> onUpdateSuccess()
                UpdateResult.NoUpdatesMade -> {}
            }

            status.emit(UpdatePaymentMethodInteractor.Status.Idle)
        }
    }

    private suspend fun maybeUpdateCardBrand(): Result<PaymentMethod>? {
        val newCardBrand = cardBrandChoice.value.brand
        return if (cardBrandHasBeenChanged.value) {
            updatePaymentMethodExecutor(
                displayableSavedPaymentMethod.paymentMethod,
                CardUpdateParams(
                    cardBrand = newCardBrand
                )
            ).onSuccess {
                savedCardBrand.emit(CardBrandChoice(brand = newCardBrand, enabled = true))
                cardBrandHasBeenChanged.emit(false)
            }
        } else {
            null
        }
    }

    private suspend fun maybeSetDefaultPaymentMethod(): Result<Unit>? {
        return if (setAsDefaultCheckboxChecked.value) {
            setDefaultPaymentMethodExecutor(displayableSavedPaymentMethod.paymentMethod)
        } else {
            null
        }
    }

    private fun getUpdateResult(
        updateCardBrandResult: Result<PaymentMethod>?,
        setDefaultPaymentMethodResult: Result<Unit>?,
    ): UpdateResult {
        if (updateCardBrandResult == null && setDefaultPaymentMethodResult == null) {
            return UpdateResult.NoUpdatesMade
        }

        return if (updateCardBrandResult?.isFailure == true && setDefaultPaymentMethodResult?.isFailure == true) {
            UpdateResult.Error(updatesFailedErrorMessage)
        } else if (updateCardBrandResult?.isFailure == true) {
            UpdateResult.Error(updateCardBrandErrorMessage)
        } else if (setDefaultPaymentMethodResult?.isFailure == true) {
            UpdateResult.Error(setDefaultPaymentMethodErrorMessage)
        } else {
            UpdateResult.Success
        }
    }

    private fun onBrandChoiceChanged(cardBrandChoice: CardBrandChoice) {
        this.cardBrandChoice.value = cardBrandChoice
        val changed = cardBrandChoice != savedCardBrand.value
        this.cardBrandHasBeenChanged.value = changed

        if (changed) {
            onBrandChoiceSelected(cardBrandChoice.brand)
        }
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

    sealed class UpdateResult {
        data class Error(val errorMessage: ResolvableString?) : UpdateResult()
        data object Success : UpdateResult()
        data object NoUpdatesMade : UpdateResult()
    }

    companion object {

        @VisibleForTesting
        internal val setDefaultPaymentMethodErrorMessage =
            R.string.stripe_paymentsheet_set_default_payment_method_failed_error_message.resolvableString

        @VisibleForTesting
        internal val updateCardBrandErrorMessage =
            R.string.stripe_paymentsheet_set_default_payment_method_failed_error_message.resolvableString

        @VisibleForTesting
        internal val updatesFailedErrorMessage =
            R.string.stripe_paymentsheet_card_updates_failed_error_message.resolvableString
    }
}

internal const val PaymentMethodRemovalDelayMillis = 600L
