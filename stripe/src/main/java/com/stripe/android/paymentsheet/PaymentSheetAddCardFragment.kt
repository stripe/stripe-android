package com.stripe.android.paymentsheet

import androidx.fragment.app.activityViewModels

internal class PaymentSheetAddCardFragment : BaseAddCardFragment() {
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
}
