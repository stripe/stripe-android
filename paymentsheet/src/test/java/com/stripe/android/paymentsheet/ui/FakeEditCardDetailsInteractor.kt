package com.stripe.android.paymentsheet.ui

import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.ViewActionRecorder
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow

internal class FakeEditCardDetailsInteractor(
    private val payload: EditCardPayload = EditCardPayload.create(PaymentMethodFixtures.CARD_WITH_NETWORKS, null),
    private val shouldShowCardBrandDropdown: Boolean = true,
    private val expiryDateEditEnabled: Boolean = true,
    override val state: StateFlow<EditCardDetailsInteractor.State> = stateFlowOf(
        EditCardDetailsInteractor.State(
            payload = payload,
            paymentMethodIcon = payload.getSavedPaymentMethodIcon(forVerticalMode = true),
            cardDetailsState = EditCardDetailsInteractor.CardDetailsState(
                selectedCardBrand = payload.getPreferredChoice(DefaultCardBrandFilter),
                shouldShowCardBrandDropdown = shouldShowCardBrandDropdown,
                availableNetworks = listOf(
                    CardBrandChoice(CardBrand.CartesBancaires, enabled = true),
                    CardBrandChoice(CardBrand.Visa, enabled = true),
                ),
                expiryDateState = ExpiryDateState.create(
                    editPayload = payload,
                    enabled = expiryDateEditEnabled,
                ),
            ),
            billingDetailsForm = null,
        )
    ),
) : EditCardDetailsInteractor {
    val viewActionRecorder = ViewActionRecorder<EditCardDetailsInteractor.ViewAction>()

    override fun handleViewAction(viewAction: EditCardDetailsInteractor.ViewAction) {
        viewActionRecorder.record(viewAction)
    }
}
