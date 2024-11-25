package com.stripe.android.paymentsheet.ui

import com.stripe.android.CardBrandFilter
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
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

    val state: StateFlow<State>

    data class State(
        val error: ResolvableString?,
        val isRemoving: Boolean,
    )

    fun handleViewAction(viewAction: ViewAction)

    sealed class ViewAction {
        data object RemovePaymentMethod : ViewAction()
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

internal class DefaultUpdatePaymentMethodInteractor(
    override val isLiveMode: Boolean,
    override val canRemove: Boolean,
    override val displayableSavedPaymentMethod: DisplayableSavedPaymentMethod,
    override val cardBrandFilter: CardBrandFilter,
    private val removeExecutor: PaymentMethodRemoveOperation,
    workContext: CoroutineContext = Dispatchers.Default,
) : UpdatePaymentMethodInteractor {
    private val coroutineScope = CoroutineScope(workContext + SupervisorJob())
    private val error = MutableStateFlow(getInitialError())
    private val isRemoving = MutableStateFlow(false)

    override val isExpiredCard = paymentMethodIsExpiredCard()
    override val screenTitle: ResolvableString? = UpdatePaymentMethodInteractor.screenTitle(
        displayableSavedPaymentMethod
    )

    private val _state = combineAsStateFlow(
        error,
        isRemoving,
    ) { error, isRemoving ->
        UpdatePaymentMethodInteractor.State(
            error = error,
            isRemoving = isRemoving,
        )
    }
    override val state = _state

    override fun handleViewAction(viewAction: UpdatePaymentMethodInteractor.ViewAction) {
        when (viewAction) {
            UpdatePaymentMethodInteractor.ViewAction.RemovePaymentMethod -> {
                coroutineScope.launch {
                    error.emit(getInitialError())
                    isRemoving.emit(true)

                    val removeError = removeExecutor(displayableSavedPaymentMethod.paymentMethod)

                    isRemoving.emit(false)
                    error.emit(removeError?.stripeErrorMessage() ?: getInitialError())
                }
            }
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
