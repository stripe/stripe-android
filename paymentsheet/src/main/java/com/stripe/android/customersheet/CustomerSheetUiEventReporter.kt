package com.stripe.android.customersheet

import com.stripe.android.ui.core.analytics.PaymentsUiEventReporter

internal object CustomerSheetUiEventReporter : PaymentsUiEventReporter {
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
