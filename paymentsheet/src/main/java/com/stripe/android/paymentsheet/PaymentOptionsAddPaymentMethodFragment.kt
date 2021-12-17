package com.stripe.android.paymentsheet

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.paymentsheet.analytics.EventReporter

internal class PaymentOptionsAddPaymentMethodFragment(
    eventReporter: EventReporter? = null
) : BaseAddPaymentMethodFragment(eventReporter) {
    override val viewModelFactory: ViewModelProvider.Factory = PaymentOptionsViewModel.Factory(
        { requireActivity().application },
        {
            requireNotNull(
                requireArguments().getParcelable(PaymentOptionsActivity.EXTRA_STARTER_ARGS)
            )
        },
        activity as AppCompatActivity
    )

    override val sheetViewModel by activityViewModels<PaymentOptionsViewModel> {
        viewModelFactory
    }
}
