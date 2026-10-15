package com.stripe.android.checkout.analytics

import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse

internal fun CheckoutSessionResponse.checkoutAnalyticsParams(): Map<String, Any> = buildMap<String, Any> {
    put("checkout_session_id", id)
    configId?.let { put("checkout_config_id", it) }
    put("currency", currency)
    put("session_status", status.name.lowercase())
    put("payment_status", paymentStatus.name.lowercase())
    elementsSession?.elementsSessionConfigId
        ?.takeIf { it.isNotEmpty() }
        ?.let { put("elements_session_config_id", it) }
    put(
        "adaptive_pricing_active",
        adaptivePricingInfo != null &&
            adaptivePricingInfo.activePresentmentCurrency != adaptivePricingInfo.integrationCurrency,
    )
    put("automatic_tax_enabled", automaticTaxEnabled)
}
