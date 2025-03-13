package com.stripe.android.paymentsheet.ui

import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod

internal data class CardUpdateParams(
    val expiryMonth: Int? = null,
    val expiryYear: Int? = null,
    val cardBrand: CardBrand? = null,
    val billingDetails: PaymentMethod.BillingDetails? = null,
)