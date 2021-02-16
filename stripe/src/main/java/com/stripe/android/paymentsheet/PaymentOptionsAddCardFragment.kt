package com.stripe.android.paymentsheet

import androidx.fragment.app.activityViewModels
import com.stripe.android.R
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.FragmentConfig

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

    override fun onGooglePaySelected() {
        sheetViewModel.onUserSelection()
    }

    override fun createHeaderText(
        config: FragmentConfig
    ): String {
        return getString(R.string.stripe_paymentsheet_add_payment_method_title)
    }
}
