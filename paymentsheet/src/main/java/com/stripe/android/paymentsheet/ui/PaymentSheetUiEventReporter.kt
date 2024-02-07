package com.stripe.android.paymentsheet.ui

import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.ui.core.analytics.PaymentsUiEventReporter

internal class PaymentSheetUiEventReporter(
    private val viewModel: BaseSheetViewModel
) : PaymentsUiEventReporter {
    override fun onCardNumberCompleted() {
        // TODO(samer-stripe): Connect to base sheet view model
    }

    override fun onAutofillEvent(type: String) {
        viewModel.reportAutofillEvent(type)
    }

    override fun onFieldInteracted() {
        // TODO(samer-stripe): Connect to base sheet view model
    }
}
