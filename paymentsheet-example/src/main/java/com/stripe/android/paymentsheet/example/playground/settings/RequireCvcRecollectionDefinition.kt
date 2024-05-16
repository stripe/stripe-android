package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest

internal object RequireCvcRecollectionDefinition : BooleanSettingsDefinition(
    key = "requireCvcRecollection",
    displayName = "Require CVC Recollection",
    defaultValue = false
) {

    override fun configure(value: Boolean, checkoutRequestBuilder: CheckoutRequest.Builder) {
        checkoutRequestBuilder.requireCvcRecollection(value)
    }
}
