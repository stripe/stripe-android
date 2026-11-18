package com.stripe.android.link

import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.LinkMode

internal fun LinkConfiguration.isIndiaCardOverride(details: ConsumerPaymentDetails.PaymentDetails): Boolean {
    val isPaymentMethodMode = !passthroughModeEnabled &&
        (linkMode == null || linkMode == LinkMode.LinkPaymentMethod)
    val country = (details as? ConsumerPaymentDetails.Card)?.country?.uppercase()
    return isPaymentMethodMode && flags["link_route_india_card_to_passthrough"] == true &&
        (country == "IN" || country == "COUNTRY_IN")
}

internal fun LinkConfiguration.shouldSharePaymentDetails(details: ConsumerPaymentDetails.PaymentDetails): Boolean {
    return passthroughModeEnabled || isIndiaCardOverride(details)
}

/** Distinguishes a failed India share from creation failures that allow inline signup's raw-card fallback. */
internal class IndiaCardShareException(cause: Throwable) : Exception(cause)
