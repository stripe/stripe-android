package com.stripe.android.paymentsheet

import androidx.fragment.app.activityViewModels
import com.stripe.android.paymentsheet.viewmodels.SheetViewModel

internal class PaymentSheetAddCardFragment : BaseAddCardFragment() {
    override val sheetViewModel: SheetViewModel<PaymentSheetViewModel.TransitionTarget> by activityViewModels<PaymentSheetViewModel> {
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
