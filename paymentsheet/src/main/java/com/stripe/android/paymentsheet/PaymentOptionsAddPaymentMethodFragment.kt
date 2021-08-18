package com.stripe.android.paymentsheet

import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.paymentsheet.analytics.EventReporter

internal class PaymentOptionsAddPaymentMethodFragment(
    eventReporter: EventReporter
) : com.stripe.android.paymentsheet.BaseAddPaymentMethodFragment(eventReporter) {
    override val viewModelFactory: ViewModelProvider.Factory = PaymentOptionsViewModel.Factory(
        { requireActivity().application },
        {
            requireNotNull(
                requireArguments().getParcelable(PaymentOptionsActivity.EXTRA_STARTER_ARGS)
            )
        }
    )

    override val sheetViewModel by activityViewModels<PaymentOptionsViewModel> {
        viewModelFactory
    }
}
