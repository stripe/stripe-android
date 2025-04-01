package com.stripe.android.paymentsheet.ui

import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.ViewActionRecorder
import com.stripe.android.uicore.elements.DateConfig
import com.stripe.android.uicore.elements.TextFieldState
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow

internal class FakeEditCardDetailsInteractor(
    private val card: PaymentMethod.Card = PaymentMethodFixtures.CARD_WITH_NETWORKS,
    private val shouldShowCardBrandDropdown: Boolean = true,
    private val shouldAllowExpDateEdit: Boolean = true,
    private val dateValidator: (String) -> TextFieldState = {
        DateConfig().determineState(it)
    },
    override val state: StateFlow<EditCardDetailsInteractor.State> = stateFlowOf(
        EditCardDetailsInteractor.State(
            card = card,
            selectedCardBrand = card.getPreferredChoice(DefaultCardBrandFilter),
            paymentMethodIcon = card.getSavedPaymentMethodIcon(forVerticalMode = true),
            shouldShowCardBrandDropdown = shouldShowCardBrandDropdown,
            availableNetworks = listOf(
                CardBrandChoice(CardBrand.CartesBancaires, enabled = true),
                CardBrandChoice(CardBrand.Visa, enabled = true),
            ),
            shouldAllowExpDateEdit = shouldAllowExpDateEdit,
            dateValidator = dateValidator
        )
    ),
) : EditCardDetailsInteractor {
    val viewActionRecorder = ViewActionRecorder<EditCardDetailsInteractor.ViewAction>()

    override fun handleViewAction(viewAction: EditCardDetailsInteractor.ViewAction) {
        viewActionRecorder.record(viewAction)
    }
}
