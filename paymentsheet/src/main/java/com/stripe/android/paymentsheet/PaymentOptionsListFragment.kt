package com.stripe.android.paymentsheet

import androidx.fragment.app.activityViewModels

internal class PaymentOptionsListFragment : BasePaymentMethodsListFragment() {
    private val activityViewModel by activityViewModels<PaymentOptionsViewModel> {
        PaymentOptionsViewModel.Factory {
            requireNotNull(
                requireArguments().getParcelable(PaymentOptionsActivity.EXTRA_STARTER_ARGS)
            )
        }
    }

    override val sheetViewModel: PaymentOptionsViewModel by lazy { activityViewModel }
}
