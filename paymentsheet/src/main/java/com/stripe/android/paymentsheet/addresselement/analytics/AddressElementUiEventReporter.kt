package com.stripe.android.paymentsheet.addresselement.analytics

import com.stripe.android.ui.core.analytics.PaymentsUiEventReporter

internal object AddressElementUiEventReporter : PaymentsUiEventReporter {
    override fun onCardNumberCompleted() {
        // No-op
    }

    override fun onAutofillEvent(type: String) {
        // No-op
    }

    override fun onFieldInteracted() {
        // No-op
    }
}
