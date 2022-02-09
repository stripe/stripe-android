package com.stripe.android.paymentsheet

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider

internal class PaymentOptionsAddPaymentMethodFragment : BaseAddPaymentMethodFragment() {
    override val viewModelFactory: ViewModelProvider.Factory = PaymentOptionsViewModel.Factory(
        { requireActivity().application },
        {
            requireNotNull(
                requireArguments().getParcelable(PaymentOptionsActivity.EXTRA_STARTER_ARGS)
            )
        },
        (activity as? AppCompatActivity) ?: this
    )

    override val sheetViewModel by activityViewModels<PaymentOptionsViewModel> {
        viewModelFactory
    }
}
