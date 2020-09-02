package com.stripe.android.paymentsheet.model

import androidx.annotation.DrawableRes

data class PaymentOption(
    @DrawableRes val drawableResourceId: Int,
    val label: String
)
