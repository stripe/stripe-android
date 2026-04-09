package com.stripe.android.paymentsheet.state

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.model.PaymentMethodMessagePromotion
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.repositories.PaymentMethodMessagePromotionsHelper

class FakePaymentMethodMessagePromotionsHelper(
    private val promotions: List<PaymentMethodMessagePromotion>? = null
) : PaymentMethodMessagePromotionsHelper {
    private val _calls = Turbine<Unit>()
    val calls: ReceiveTurbine<Unit> = _calls

    override fun fetchPromotionsAsync(intent: StripeIntent) {
        _calls.add(Unit)
    }

    override fun getPromotions(): List<PaymentMethodMessagePromotion>? {
        return promotions
    }

    fun validate() {
        calls.ensureAllEventsConsumed()
    }
}
