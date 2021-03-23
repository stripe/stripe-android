package com.stripe.android.paymentsheet

import androidx.fragment.app.activityViewModels
import com.stripe.android.paymentsheet.analytics.EventReporter

internal class PaymentOptionsAddCardFragment(
    eventReporter: EventReporter
) : BaseAddCardFragment(eventReporter) {
    override val sheetViewModel by activityViewModels<PaymentOptionsViewModel> {
        PaymentOptionsViewModel.Factory(
            { requireActivity().application },
            {
                requireNotNull(
                    requireArguments().getParcelable(PaymentOptionsActivity.EXTRA_STARTER_ARGS)
                )
            }
        )
    }
}
