package com.stripe.android.paymentsheet.ui

import androidx.compose.runtime.Immutable
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.CardUpdateParams
import kotlinx.coroutines.flow.StateFlow

internal typealias CardUpdateParamsCallback = (CardUpdateParams?) -> Unit

internal typealias CardBrandCallback = (CardBrand) -> Unit

internal interface EditCardDetailsInteractor {
    val state: StateFlow<State>

    val onCardUpdateParamsChanged: CardUpdateParamsCallback

    fun handleViewAction(viewAction: ViewAction)

    @Immutable
    data class State(
        val card: PaymentMethod.Card,
        val selectedCardBrand: CardBrandChoice,
        val paymentMethodIcon: Int,
        val shouldShowCardBrandDropdown: Boolean,
        val availableNetworks: List<CardBrandChoice>
    )

    sealed interface ViewAction {
        data class BrandChoiceChanged(val cardBrandChoice: CardBrandChoice) : ViewAction
    }

    fun interface Factory {
        fun create(
            card: PaymentMethod.Card,
            onCardUpdateParamsChanged: CardUpdateParamsCallback
        ): EditCardDetailsInteractor
    }
}
