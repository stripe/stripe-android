package com.stripe.android.paymentsheet.ui

import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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
    fun close()

    interface Factory {
        fun create(
            initialPaymentMethod: PaymentMethod,
            eventHandler: (EditPaymentMethodViewInteractor.Event) -> Unit,
            removeExecutor: PaymentMethodRemoveOperation,
            updateExecutor: PaymentMethodUpdateOperation,
            displayName: String,
        ): ModifiableEditPaymentMethodViewInteractor
    }
}

internal class DefaultEditPaymentMethodViewInteractor constructor(
    initialPaymentMethod: PaymentMethod,
    displayName: String,
    private val eventHandler: (EditPaymentMethodViewInteractor.Event) -> Unit,
    private val removeExecutor: PaymentMethodRemoveOperation,
    private val updateExecutor: PaymentMethodUpdateOperation,
    workContext: CoroutineContext = Dispatchers.Default,
    viewStateSharingStarted: SharingStarted = SharingStarted.WhileSubscribed()
) : ModifiableEditPaymentMethodViewInteractor, CoroutineScope {
    private val choice = MutableStateFlow(initialPaymentMethod.getPreferredChoice())
    private val status = MutableStateFlow(EditPaymentMethodViewState.Status.Idle)
    private val paymentMethod = MutableStateFlow(initialPaymentMethod)
    private val confirmRemoval = MutableStateFlow(false)
    private val error = MutableStateFlow<ResolvableString?>(null)

    override val coroutineContext: CoroutineContext = workContext + SupervisorJob()

    override val viewState = combine(
        paymentMethod,
        choice,
        status,
        confirmRemoval,
        error,
    ) { paymentMethod, choice, status, confirmDeletion, error ->
        val savedChoice = paymentMethod.getPreferredChoice()
        val availableChoices = paymentMethod.getAvailableNetworks()

        EditPaymentMethodViewState(
            last4 = paymentMethod.getLast4(),
            canUpdate = savedChoice != choice,
            selectedBrand = choice,
            availableBrands = availableChoices,
            status = status,
            displayName = displayName,
            error = error,
            confirmRemoval = confirmDeletion,
        )
    }.stateIn(
        scope = this,
        started = viewStateSharingStarted,
        initialValue = EditPaymentMethodViewState(
            last4 = initialPaymentMethod.getLast4(),
            selectedBrand = initialPaymentMethod.getPreferredChoice(),
            canUpdate = false,
            availableBrands = initialPaymentMethod.getAvailableNetworks(),
            status = EditPaymentMethodViewState.Status.Idle,
            displayName = displayName,
        )
    )

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
        cancel()
    }

    private fun onRemovePressed() {
        confirmRemoval.value = true
    }

    private fun onRemoveConfirmed() {
        confirmRemoval.value = false

        launch {
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

        if (currentPaymentMethod.getPreferredChoice() != currentChoice) {
            launch {
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

    private fun onBrandChoiceChanged(choice: EditPaymentMethodViewState.CardBrandChoice) {
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

    private fun PaymentMethod.getPreferredChoice(): EditPaymentMethodViewState.CardBrandChoice {
        return (getCard().displayBrand?.type ?: CardBrand.Unknown).toChoice()
    }

    private fun PaymentMethod.getAvailableNetworks(): List<EditPaymentMethodViewState.CardBrandChoice> {
        return getCard().networks?.available?.let { brandCodes ->
            brandCodes.map { code ->
                CardBrand.fromCode(code).toChoice()
            }
        } ?: listOf()
    }

    private fun PaymentMethod.getCard(): PaymentMethod.Card {
        return card ?: throw IllegalStateException("Payment method must be a card in order to be edited")
    }

    private fun CardBrand.toChoice(): EditPaymentMethodViewState.CardBrandChoice {
        return EditPaymentMethodViewState.CardBrandChoice(brand = this)
    }

    object Factory : ModifiableEditPaymentMethodViewInteractor.Factory {
        override fun create(
            initialPaymentMethod: PaymentMethod,
            eventHandler: (EditPaymentMethodViewInteractor.Event) -> Unit,
            removeExecutor: PaymentMethodRemoveOperation,
            updateExecutor: PaymentMethodUpdateOperation,
            displayName: String,
        ): ModifiableEditPaymentMethodViewInteractor {
            return DefaultEditPaymentMethodViewInteractor(
                initialPaymentMethod = initialPaymentMethod,
                eventHandler = eventHandler,
                removeExecutor = removeExecutor,
                updateExecutor = updateExecutor,
                displayName = displayName,
            )
        }
    }
}
