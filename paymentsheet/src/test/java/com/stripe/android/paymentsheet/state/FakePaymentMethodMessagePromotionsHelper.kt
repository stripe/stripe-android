package com.stripe.android.utils

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.model.PaymentMethodCode
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

    override fun getPromotionIfAvailableForCode(code: PaymentMethodCode): PaymentMethodMessagePromotion? {
        return promotions?.find {
            it.paymentMethodType.lowercase() == code
        }
    }

    fun validate() {
        calls.ensureAllEventsConsumed()
    }
}
