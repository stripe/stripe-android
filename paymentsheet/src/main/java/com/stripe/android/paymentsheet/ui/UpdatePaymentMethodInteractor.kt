package com.stripe.android.paymentsheet.ui

import androidx.annotation.VisibleForTesting
import com.stripe.android.CardBrandFilter
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.model.LinkPaymentDetails
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.CardUpdateParams
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.SavedPaymentMethod
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.mapAsStateFlow
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
    val removeMessage: ResolvableString?
    val screenTitle: ResolvableString?
    val cardBrandFilter: CardBrandFilter
    val isExpiredCard: Boolean
    val isModifiablePaymentMethod: Boolean
    val hasValidBrandChoices: Boolean
    val shouldShowSetAsDefaultCheckbox: Boolean
    val setAsDefaultCheckboxEnabled: Boolean
    val shouldShowSaveButton: Boolean
    val canUpdateFullPaymentMethodDetails: Boolean
    val addressCollectionMode: AddressCollectionMode
    val allowedBillingCountries: Set<String>
    val editCardDetailsInteractor: EditCardDetailsInteractor

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
        data object SaveButtonPressed : ViewAction()
        data object DisabledSaveButtonPressed : ViewAction()
        data class SetAsDefaultCheckboxChanged(val isChecked: Boolean) : ViewAction()
        data class CardUpdateParamsChanged(val cardUpdateParams: CardUpdateParams?) : ViewAction()
    }

    companion object {
        fun screenTitle(displayableSavedPaymentMethod: DisplayableSavedPaymentMethod) = (
            when (displayableSavedPaymentMethod.savedPaymentMethod) {
                is SavedPaymentMethod.SepaDebit -> R.string.stripe_paymentsheet_manage_sepa_debit
                is SavedPaymentMethod.USBankAccount -> R.string.stripe_paymentsheet_manage_bank_account
                is SavedPaymentMethod.Card -> R.string.stripe_paymentsheet_manage_card
                is SavedPaymentMethod.Link -> {
                    when (displayableSavedPaymentMethod.paymentMethod.linkPaymentDetails) {
                        is LinkPaymentDetails.BankAccount -> {
                            R.string.stripe_paymentsheet_manage_international_bank_account
                        }
                        is LinkPaymentDetails.Card -> {
                            R.string.stripe_paymentsheet_manage_card
                        }
                        null -> null
                    }
                }
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
    override val addressCollectionMode: AddressCollectionMode,
    override val allowedBillingCountries: Set<String>,
    override val canUpdateFullPaymentMethodDetails: Boolean,
    override val removeMessage: ResolvableString?,
    val isDefaultPaymentMethod: Boolean,
    override val shouldShowSetAsDefaultCheckbox: Boolean,
    private val removeExecutor: PaymentMethodRemoveOperation,
    private val updatePaymentMethodExecutor: UpdateCardPaymentMethodOperation,
    private val setDefaultPaymentMethodExecutor: PaymentMethodSetAsDefaultOperation,
    private val onBrandChoiceSelected: (CardBrand) -> Unit,
    private val onUpdateSuccess: () -> Unit,
    val editCardDetailsInteractorFactory: EditCardDetailsInteractor.Factory = DefaultEditCardDetailsInteractor
        .Factory(),
) : UpdatePaymentMethodInteractor {
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val error = MutableStateFlow(getInitialError())
    private val status = MutableStateFlow(UpdatePaymentMethodInteractor.Status.Idle)
    private val initialSetAsDefaultCheckedValue = isDefaultPaymentMethod
    private val setAsDefaultCheckboxChecked = MutableStateFlow(initialSetAsDefaultCheckedValue)
    private val cardUpdateParams = MutableStateFlow<CardUpdateParams?>(null)

    override val hasValidBrandChoices = hasValidBrandChoices()
    override val isExpiredCard = paymentMethodIsExpiredCard()
    override val screenTitle: ResolvableString? = UpdatePaymentMethodInteractor.screenTitle(
        displayableSavedPaymentMethod
    )
    override val isModifiablePaymentMethod: Boolean
        get() = displayableSavedPaymentMethod.isModifiable(canUpdateFullPaymentMethodDetails)

    override val topBarState: PaymentSheetTopBarState = PaymentSheetTopBarStateFactory.create(
        isLiveMode = isLiveMode,
        editable = PaymentSheetTopBarState.Editable.Never,
    )
    override val setAsDefaultCheckboxEnabled: Boolean = !isDefaultPaymentMethod

    override val shouldShowSaveButton: Boolean = isModifiablePaymentMethod ||
        (shouldShowSetAsDefaultCheckbox && !isDefaultPaymentMethod)

    private val _setAsDefaultValueChanged = setAsDefaultCheckboxChecked.mapAsStateFlow { setAsDefaultCheckboxChecked ->
        setAsDefaultCheckboxChecked != initialSetAsDefaultCheckedValue
    }
    override val editCardDetailsInteractor by lazy {
        when (val paymentMethod = displayableSavedPaymentMethod.savedPaymentMethod) {
            is SavedPaymentMethod.Card -> {
                createEditCardDetailsInteractorForCard(paymentMethod)
            }
            is SavedPaymentMethod.Link -> {
                val linkPaymentDetails = paymentMethod.paymentDetails as? LinkPaymentDetails.Card
                    ?: throw IllegalArgumentException("Link payment method is not a card")
                createEditCardDetailsInteractorForLink(linkPaymentDetails)
            }
            else -> {
                throw IllegalArgumentException(
                    "Card or Link payment method required for creating EditCardDetailsInteractor"
                )
            }
        }
    }

    private fun createEditCardDetailsInteractorForCard(
        savedPaymentMethodCard: SavedPaymentMethod.Card,
    ): EditCardDetailsInteractor {
        val isModifiable = displayableSavedPaymentMethod.isModifiable(canUpdateFullPaymentMethodDetails)
        val payload = EditCardPayload.create(savedPaymentMethodCard.card, savedPaymentMethodCard.billingDetails)
        val cardEditConfiguration = CardEditConfiguration(
            cardBrandFilter = cardBrandFilter,
            isCbcModifiable = isModifiable && displayableSavedPaymentMethod.canChangeCbc(),
            areExpiryDateAndAddressModificationSupported = isModifiable && canUpdateFullPaymentMethodDetails,
        )
        return editCardDetailsInteractorFactory.create(
            payload = payload,
            cardEditConfiguration = cardEditConfiguration,
            onCardUpdateParamsChanged = { cardUpdateParams ->
                onCardUpdateParamsChanged(cardUpdateParams)
            },
            coroutineScope = coroutineScope,
            onBrandChoiceChanged = onBrandChoiceSelected,
            // name, email, and phone are purposefully omitted (not collected) here.
            billingDetailsCollectionConfiguration = BillingDetailsCollectionConfiguration(
                address = addressCollectionMode,
                email = CollectionMode.Never,
                phone = CollectionMode.Never,
                name = CollectionMode.Never,
                allowedCountries = allowedBillingCountries,
            ),
            requiresModification = true
        )
    }

    private fun createEditCardDetailsInteractorForLink(
        savedPaymentMethodCard: LinkPaymentDetails.Card,
    ): EditCardDetailsInteractor {
        val payload = EditCardPayload.create(savedPaymentMethodCard)
        val cardEditConfiguration = CardEditConfiguration(
            cardBrandFilter = cardBrandFilter,
            isCbcModifiable = false,
            areExpiryDateAndAddressModificationSupported = false,
        )
        return editCardDetailsInteractorFactory.create(
            payload = payload,
            cardEditConfiguration = cardEditConfiguration,
            onCardUpdateParamsChanged = { cardUpdateParams ->
                onCardUpdateParamsChanged(cardUpdateParams)
            },
            coroutineScope = coroutineScope,
            onBrandChoiceChanged = onBrandChoiceSelected,
            billingDetailsCollectionConfiguration = BillingDetailsCollectionConfiguration(
                address = AddressCollectionMode.Never,
                email = CollectionMode.Never,
                phone = CollectionMode.Never,
                name = CollectionMode.Never,
                allowedCountries = allowedBillingCountries,
            ),
            requiresModification = true
        )
    }

    private fun onCardUpdateParamsChanged(cardUpdateParams: CardUpdateParams?) {
        this.cardUpdateParams.value = cardUpdateParams
    }

    private val _state = combineAsStateFlow(
        error,
        status,
        setAsDefaultCheckboxChecked,
        _setAsDefaultValueChanged,
        cardUpdateParams
    ) { error,
        status,
        setAsDefaultCheckboxChecked,
        setAsDefaultValueChanged,
        cardUpdateParams ->
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
            UpdatePaymentMethodInteractor.ViewAction.DisabledSaveButtonPressed -> validate()
            is UpdatePaymentMethodInteractor.ViewAction.SetAsDefaultCheckboxChanged -> onSetAsDefaultCheckboxChanged(
                isChecked = viewAction.isChecked
            )
            is UpdatePaymentMethodInteractor.ViewAction.CardUpdateParamsChanged -> {
                onCardUpdateParamsChanged(viewAction.cardUpdateParams)
            }
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

    private fun validate() {
        editCardDetailsInteractor.handleViewAction(EditCardDetailsInteractor.ViewAction.Validate)
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
        return if (_setAsDefaultValueChanged.value && setAsDefaultCheckboxChecked.value) {
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
        return if (paymentMethodIsExpiredCard() && !isModifiablePaymentMethod) {
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
