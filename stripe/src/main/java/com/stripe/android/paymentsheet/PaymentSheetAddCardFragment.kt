package com.stripe.android.paymentsheet

import androidx.fragment.app.activityViewModels
import com.stripe.android.paymentsheet.analytics.EventReporter

internal class PaymentSheetAddCardFragment(
    eventReporter: EventReporter
) : BaseAddCardFragment(eventReporter) {
    override val sheetViewModel by activityViewModels<PaymentSheetViewModel> {
        PaymentSheetViewModel.Factory(
            { requireActivity().application },
            {
                requireNotNull(
                    requireArguments().getParcelable(PaymentSheetActivity.EXTRA_STARTER_ARGS)
                )
            }
        )
    }

    override fun onGooglePaySelected() {
        sheetViewModel.checkout()
    }
}
