package com.stripe.android.paymentsheet.ui

import com.stripe.android.CardBrandFilter
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.uicore.utils.combineAsStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

internal typealias PaymentMethodRemoveOperation = suspend (paymentMethod: PaymentMethod) -> Throwable?
internal typealias PaymentMethodUpdateOperation = suspend (
    paymentMethod: PaymentMethod,
    brand: CardBrand
) -> Result<PaymentMethod>

internal const val PaymentMethodRemovalDelayMillis = 600L

internal interface EditPaymentMethodViewInteractor {
    val viewState: StateFlow<EditPaymentMethodViewState>

    fun handleViewAction(viewAction: EditPaymentMethodViewAction)

    sealed interface Event {
        data class ShowBrands(val brand: CardBrand) : Event

        data class HideBrands(val brand: CardBrand?) : Event
    }
}

internal interface ModifiableEditPaymentMethodViewInteractor : EditPaymentMethodViewInteractor {
    val isLiveMode: Boolean

    fun close()

    interface Factory {
        fun create(
            initialPaymentMethod: PaymentMethod,
            eventHandler: (EditPaymentMethodViewInteractor.Event) -> Unit,
            removeExecutor: PaymentMethodRemoveOperation,
            updateExecutor: PaymentMethodUpdateOperation,
            displayName: ResolvableString,
            canRemove: Boolean,
            isLiveMode: Boolean,
            cardBrandFilter: CardBrandFilter
        ): ModifiableEditPaymentMethodViewInteractor
    }
}

internal class DefaultEditPaymentMethodViewInteractor(
    initialPaymentMethod: PaymentMethod,
    displayName: ResolvableString,
    private val eventHandler: (EditPaymentMethodViewInteractor.Event) -> Unit,
    private val removeExecutor: PaymentMethodRemoveOperation,
    private val updateExecutor: PaymentMethodUpdateOperation,
    private val canRemove: Boolean,
    override val isLiveMode: Boolean,
    private val cardBrandFilter: CardBrandFilter,
    workContext: CoroutineContext = Dispatchers.Default,
) : ModifiableEditPaymentMethodViewInteractor {
    private val choice = MutableStateFlow(initialPaymentMethod.getCard().getPreferredChoice())
    private val status = MutableStateFlow(EditPaymentMethodViewState.Status.Idle)
    private val paymentMethod = MutableStateFlow(initialPaymentMethod)
    private val confirmRemoval = MutableStateFlow(false)
    private val error = MutableStateFlow<ResolvableString?>(null)
    private val coroutineScope = CoroutineScope(workContext + SupervisorJob())

    override val viewState = combineAsStateFlow(
        paymentMethod,
        choice,
        status,
        confirmRemoval,
        error,
    ) { paymentMethod, choice, status, confirmDeletion, error ->
        val savedChoice = paymentMethod.getCard().getPreferredChoice()
        val availableChoices = paymentMethod.getCard().getAvailableNetworks(cardBrandFilter)

        EditPaymentMethodViewState(
            last4 = paymentMethod.getLast4(),
            canUpdate = savedChoice != choice,
            selectedBrand = choice,
            availableBrands = availableChoices,
            status = status,
            displayName = displayName,
            error = error,
            confirmRemoval = confirmDeletion,
            canRemove = canRemove,
        )
    }

    override fun handleViewAction(viewAction: EditPaymentMethodViewAction) {
        when (viewAction) {
            is EditPaymentMethodViewAction.OnRemovePressed -> onRemovePressed()
            is EditPaymentMethodViewAction.OnRemoveConfirmed -> onRemoveConfirmed()
            is EditPaymentMethodViewAction.OnUpdatePressed -> onUpdatePressed()
            is EditPaymentMethodViewAction.OnBrandChoiceOptionsShown -> onBrandChoiceOptionsShown()
            is EditPaymentMethodViewAction.OnBrandChoiceOptionsDismissed -> onBrandChoiceOptionsDismissed()
            is EditPaymentMethodViewAction.OnBrandChoiceChanged -> onBrandChoiceChanged(viewAction.choice)
            is EditPaymentMethodViewAction.OnRemoveConfirmationDismissed -> onRemoveConfirmationDismissed()
        }
    }

    override fun close() {
        coroutineScope.cancel()
    }

    private fun onRemovePressed() {
        confirmRemoval.value = true
    }

    private fun onRemoveConfirmed() {
        confirmRemoval.value = false

        coroutineScope.launch {
            error.emit(null)
            status.emit(EditPaymentMethodViewState.Status.Removing)

            val paymentMethod = paymentMethod.value
            val removeError = removeExecutor(paymentMethod)

            error.emit(removeError?.stripeErrorMessage())
            status.emit(EditPaymentMethodViewState.Status.Idle)
        }
    }

    private fun onUpdatePressed() {
        val currentPaymentMethod = paymentMethod.value
        val currentChoice = choice.value

        if (currentPaymentMethod.getCard().getPreferredChoice() != currentChoice) {
            coroutineScope.launch {
                error.emit(null)
                status.emit(EditPaymentMethodViewState.Status.Updating)

                val updateResult = updateExecutor(paymentMethod.value, currentChoice.brand)

                updateResult.onSuccess { method ->
                    paymentMethod.emit(method)
                }.onFailure { throwable ->
                    error.emit(throwable.stripeErrorMessage())
                }

                status.emit(EditPaymentMethodViewState.Status.Idle)
            }
        }
    }

    private fun onBrandChoiceOptionsShown() {
        eventHandler(EditPaymentMethodViewInteractor.Event.ShowBrands(choice.value.brand))
    }

    private fun onBrandChoiceOptionsDismissed() {
        eventHandler(EditPaymentMethodViewInteractor.Event.HideBrands(brand = null))
    }

    private fun onBrandChoiceChanged(choice: CardBrandChoice) {
        this.choice.value = choice

        eventHandler(EditPaymentMethodViewInteractor.Event.HideBrands(brand = choice.brand))
    }

    private fun onRemoveConfirmationDismissed() {
        confirmRemoval.value = false
    }

    private fun PaymentMethod.getLast4(): String {
        return getCard().last4
            ?: throw IllegalStateException("Card payment method must contain last 4 digits")
    }

    private fun PaymentMethod.Card.getPreferredChoice(): CardBrandChoice {
        return CardBrand.fromCode(displayBrand).toChoice()
    }

    private fun PaymentMethod.Card.getAvailableNetworks(cardBrandFilter: CardBrandFilter): List<CardBrandChoice> {
        return networks?.available?.let { brandCodes ->
            brandCodes.map { code ->
                CardBrand.fromCode(code).toChoice()
            }.filter { cardBrandFilter.isAccepted(it.brand) }
        } ?: listOf()
    }

    private fun PaymentMethod.getCard(): PaymentMethod.Card {
        return card ?: throw IllegalStateException("Payment method must be a card in order to be edited")
    }

    private fun CardBrand.toChoice(): CardBrandChoice {
        return CardBrandChoice(brand = this)
    }

    object Factory : ModifiableEditPaymentMethodViewInteractor.Factory {
        override fun create(
            initialPaymentMethod: PaymentMethod,
            eventHandler: (EditPaymentMethodViewInteractor.Event) -> Unit,
            removeExecutor: PaymentMethodRemoveOperation,
            updateExecutor: PaymentMethodUpdateOperation,
            displayName: ResolvableString,
            canRemove: Boolean,
            isLiveMode: Boolean,
            cardBrandFilter: CardBrandFilter
        ): ModifiableEditPaymentMethodViewInteractor {
            return DefaultEditPaymentMethodViewInteractor(
                initialPaymentMethod = initialPaymentMethod,
                eventHandler = eventHandler,
                removeExecutor = removeExecutor,
                updateExecutor = updateExecutor,
                displayName = displayName,
                canRemove = canRemove,
                isLiveMode = isLiveMode,
                cardBrandFilter = cardBrandFilter
            )
        }
    }
}
