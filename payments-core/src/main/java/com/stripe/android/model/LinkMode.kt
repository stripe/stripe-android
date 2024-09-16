package com.stripe.android.model

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class LinkMode(val value: String) {
    Passthrough("PASSTHROUGH"),
    LinkPaymentMethod("LINK_PAYMENT_METHOD"),
    LinkCardBrand("LINK_CARD_BRAND"),
}

/**
 * Needed for backwards compatibility, as Elements uses these values for
 * analytics instead of the strings coming from the API.
 */
val LinkMode.analyticsValue: String
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    get() = when (this) {
        LinkMode.Passthrough -> "passthrough"
        LinkMode.LinkPaymentMethod -> "payment_method_mode"
        LinkMode.LinkCardBrand -> "link_card_brand"
    }
