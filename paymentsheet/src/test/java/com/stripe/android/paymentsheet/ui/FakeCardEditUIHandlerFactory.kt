package com.stripe.android.paymentsheet.ui

import com.stripe.android.CardBrandFilter
import com.stripe.android.model.PaymentMethod

internal class FakeCardEditUIHandlerFactory : CardEditUIHandler.Factory {
    lateinit var cardEditUIHandler: CardEditUIHandler
    override fun create(
        card: PaymentMethod.Card,
        cardBrandFilter: CardBrandFilter,
        showCardBrandDropdown: Boolean,
        paymentMethodIcon: Int,
        onCardDetailsChanged: CardDetailsCallback
    ): CardEditUIHandler {
        return FakeCardEditUIHandler(
            card = card,
            cardBrandFilter = cardBrandFilter,
            showCardBrandDropdown = showCardBrandDropdown,
            paymentMethodIcon = paymentMethodIcon,
            onCardDetailsChanged = onCardDetailsChanged
        ).also {
            this.cardEditUIHandler = it
        }
    }
}
