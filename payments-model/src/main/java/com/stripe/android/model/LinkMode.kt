package com.stripe.android.model

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class LinkMode(val value: String) {
    Passthrough("PASSTHROUGH"),
    LinkPaymentMethod("LINK_PAYMENT_METHOD"),
    LinkCardBrand("LINK_CARD_BRAND");

    val expectedPaymentMethodType: String
        get() = if (this == LinkCardBrand) "card" else "bank_account"
}
