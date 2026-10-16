package com.stripe.android.paymentsheet.analytics

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata

internal fun PaymentMethodMetadata?.defaultAnalyticsParams(): Map<String, Any> {
    return if (this != null) {
        analyticsMetadata.paramsMap +
            ("payment_method_orientation" to paymentMethodOrientation().name.lowercase())
    } else {
        emptyMap()
    }
}
