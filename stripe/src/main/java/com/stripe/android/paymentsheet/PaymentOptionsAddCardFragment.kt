package com.stripe.android.paymentsheet

import androidx.fragment.app.activityViewModels

internal class PaymentOptionsAddCardFragment : BaseAddCardFragment() {
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
