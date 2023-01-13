package com.stripe.android.paymentsheet

import androidx.fragment.app.activityViewModels

internal class PaymentSheetListFragment : BasePaymentMethodsListFragment(
    canClickSelectedItem = false
) {
    private val activityViewModel by activityViewModels<PaymentSheetViewModel> {
        PaymentSheetViewModel.Factory {
            requireNotNull(
                requireArguments().getParcelable(PaymentSheetActivity.EXTRA_STARTER_ARGS)
            )
        }
    }

    override val sheetViewModel: PaymentSheetViewModel by lazy { activityViewModel }
}
