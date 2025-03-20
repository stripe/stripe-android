package com.stripe.android.paymentsheet.ui

import com.stripe.android.CardBrandFilter
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.CardUpdateParams

internal class FakeCardEditUIHandlerFactory : CardEditUIHandler.Factory {
    lateinit var cardEditUIHandler: CardEditUIHandler
    override fun create(
        card: PaymentMethod.Card,
        cardBrandFilter: CardBrandFilter,
        showCardBrandDropdown: Boolean,
        paymentMethodIcon: Int,
        onCardValuesChanged: (CardUpdateParams?) -> Unit
    ): CardEditUIHandler {
        return FakeCardEditUIHandler(
            card = card,
            cardBrandFilter = cardBrandFilter,
            showCardBrandDropdown = showCardBrandDropdown,
            paymentMethodIcon = paymentMethodIcon,
            onCardValuesChanged = onCardValuesChanged
        ).also {
            this.cardEditUIHandler = it
        }
    }

}