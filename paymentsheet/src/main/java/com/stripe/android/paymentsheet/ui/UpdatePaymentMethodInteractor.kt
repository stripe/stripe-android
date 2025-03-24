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
    val cardEditUIHandlerFactory: CardEditUIHandler.Factory

    fun cardUiHandlerFactory(savedPaymentMethod: SavedPaymentMethod.Card): CardEditUIHandler

    val state: StateFlow<State>

    data class State(
        val error: ResolvableString?,
        val status: Status,
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
    override val cardEditUIHandlerFactory: CardEditUIHandler.Factory,
    private val onUpdateSuccess: () -> Unit,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
) : UpdatePaymentMethodInteractor {
    private val error = MutableStateFlow(getInitialError())
    private val status = MutableStateFlow(UpdatePaymentMethodInteractor.Status.Idle)
    private val initialSetAsDefaultCheckedValue = isDefaultPaymentMethod
    private val setAsDefaultCheckboxChecked = MutableStateFlow(initialSetAsDefaultCheckedValue)
    private val cardUpdateParams = MutableStateFlow<CardUpdateParams?>(null)

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

    override fun cardUiHandlerFactory(savedPaymentMethod: SavedPaymentMethod.Card): CardEditUIHandler {
        val cardUIHandler = cardEditUIHandlerFactory.create(
            card = savedPaymentMethod.card,
            cardBrandFilter = cardBrandFilter,
            onCardValuesChanged = {
                cardUpdateParams.value = it
            },
            showCardBrandDropdown = isModifiablePaymentMethod && displayableSavedPaymentMethod.isModifiable(),
            paymentMethodIcon = displayableSavedPaymentMethod.paymentMethod.getSavedPaymentMethodIcon(
                forVerticalMode = true
            )
        )
        return cardUIHandler
    }

    private val _state = combineAsStateFlow(
        error,
        status,
        setAsDefaultCheckboxChecked,
        cardUpdateParams
    ) { error, status, setAsDefaultCheckboxChecked, cardUpdateParams ->
        val setAsDefaultValueChanged = setAsDefaultCheckboxChecked != initialSetAsDefaultCheckedValue
        val isSaveButtonEnabled = (setAsDefaultValueChanged || cardUpdateParams != null) &&
            status == UpdatePaymentMethodInteractor.Status.Idle

        UpdatePaymentMethodInteractor.State(
            error = error,
            status = status,
            isSaveButtonEnabled = isSaveButtonEnabled,
            setAsDefaultCheckboxChecked = isDefaultPaymentMethod || setAsDefaultCheckboxChecked,
        )
    }
    override val state = _state

    override fun handleViewAction(viewAction: UpdatePaymentMethodInteractor.ViewAction) {
        when (viewAction) {
            UpdatePaymentMethodInteractor.ViewAction.RemovePaymentMethod -> removePaymentMethod()
            UpdatePaymentMethodInteractor.ViewAction.SaveButtonPressed -> savePaymentMethod()
            is UpdatePaymentMethodInteractor.ViewAction.BrandChoiceChanged -> Unit
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

            val updateCardBrandResult = maybeUpdateCard()
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

    private suspend fun maybeUpdateCard(): Result<PaymentMethod>? {
        val cardUpdateParams = cardUpdateParams.value
        return if (cardUpdateParams != null) {
            updatePaymentMethodExecutor(
                displayableSavedPaymentMethod.paymentMethod,
                cardUpdateParams
            ).onSuccess {
                this.cardUpdateParams.value = null
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

    private fun onSetAsDefaultCheckboxChanged(isChecked: Boolean) {
        setAsDefaultCheckboxChecked.update { isChecked }
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

        fun factory(
            isLiveMode: Boolean,
            canRemove: Boolean,
            isDefaultPaymentMethod: Boolean,
            displayableSavedPaymentMethod: DisplayableSavedPaymentMethod,
            cardBrandFilter: CardBrandFilter,
            shouldShowSetAsDefaultCheckbox: Boolean,
            removeExecutor: PaymentMethodRemoveOperation,
            updatePaymentMethodExecutor: UpdateCardPaymentMethodOperation,
            setDefaultPaymentMethodExecutor: PaymentMethodSetAsDefaultOperation,
            onBrandChoiceSelected: BrandChoiceCallback,
            onUpdateSuccess: () -> Unit,
            coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
            cardEditUIHandlerFactory: (BrandChoiceCallback) -> CardEditUIHandler.Factory = { brandChoiceCallback ->
                DefaultCardEditUIHandler.Factory(
                    scope = coroutineScope,
                    onBrandChoiceChanged = brandChoiceCallback
                )
            },
        ): DefaultUpdatePaymentMethodInteractor {
            return DefaultUpdatePaymentMethodInteractor(
                isLiveMode = isLiveMode,
                canRemove = canRemove,
                displayableSavedPaymentMethod = displayableSavedPaymentMethod,
                cardBrandFilter = cardBrandFilter,
                shouldShowSetAsDefaultCheckbox = shouldShowSetAsDefaultCheckbox,
                removeExecutor = removeExecutor,
                updatePaymentMethodExecutor = updatePaymentMethodExecutor,
                setDefaultPaymentMethodExecutor = setDefaultPaymentMethodExecutor,
                onUpdateSuccess = onUpdateSuccess,
                coroutineScope = coroutineScope,
                cardEditUIHandlerFactory = cardEditUIHandlerFactory.invoke(onBrandChoiceSelected),
                isDefaultPaymentMethod = isDefaultPaymentMethod,
            )
        }
    }
}

internal const val PaymentMethodRemovalDelayMillis = 600L
