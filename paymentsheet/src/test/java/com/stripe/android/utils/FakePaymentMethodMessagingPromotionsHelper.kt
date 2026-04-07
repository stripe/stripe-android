package com.stripe.android.utils

import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodMessagePromotion
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.repositories.PaymentMethodMessagingPromotionsHelper

class FakePaymentMethodMessagingPromotionsHelper(
    private val promotions: List<PaymentMethodMessagePromotion>? = null
) : PaymentMethodMessagingPromotionsHelper {
    override fun fetchPromotionsAsync(intent: StripeIntent) {
        // NO-OP
    }

    override fun getPromotionIfAvailableForCode(code: PaymentMethodCode): PaymentMethodMessagePromotion? {
        return promotions?.find {
            it.paymentMethodType.lowercase() == code
        }
    }
}