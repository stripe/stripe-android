package com.stripe.android.utils

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodMessageLearnMore
import com.stripe.android.model.PaymentMethodMessagePromotion
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.repositories.PaymentMethodMessagePromotionsHelper

internal class FakePaymentMethodMessagePromotionsHelper(
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

    override fun getPromotions(): List<PaymentMethodMessagePromotion>? {
        return promotions
    }

    fun validate() {
        calls.ensureAllEventsConsumed()
    }

    internal object Factory {
        fun create(
            promotions: List<PaymentMethodMessagePromotion>? = Companion.promotions
        ): PaymentMethodMessagePromotionsHelper {
            return FakePaymentMethodMessagePromotionsHelper(promotions)
        }
    }

    internal companion object {
        val klarnaPromotion = PaymentMethodMessagePromotion(
            paymentMethodType = "KLARNA",
            message = "This is a message",
            learnMore = PaymentMethodMessageLearnMore(
                message = "Click me",
                url = "https://www.test.com"
            )
        )
        val affirmPromotion = PaymentMethodMessagePromotion(
            paymentMethodType = "AFFIRM",
            message = "This is a message",
            learnMore = PaymentMethodMessageLearnMore(
                message = "Click me",
                url = "https://www.test.com"
            )
        )
        val afterpayPromotion = PaymentMethodMessagePromotion(
            paymentMethodType = "AFTERPAY_CLEARPAY",
            message = "This is a message",
            learnMore = PaymentMethodMessageLearnMore(
                message = "Click me",
                url = "https://www.test.com"
            )
        )
        val promotions = listOf(klarnaPromotion, affirmPromotion, afterpayPromotion)
    }
}
