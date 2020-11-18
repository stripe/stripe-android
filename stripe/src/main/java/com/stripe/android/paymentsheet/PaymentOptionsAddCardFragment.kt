package com.stripe.android.paymentsheet

import androidx.fragment.app.activityViewModels
import com.stripe.android.paymentsheet.viewmodels.SheetViewModel

internal class PaymentOptionsAddCardFragment : BaseAddCardFragment() {
    override val sheetViewModel: SheetViewModel<*> by activityViewModels<PaymentOptionsViewModel> {
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
