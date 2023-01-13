package com.stripe.android.paymentsheet

import androidx.fragment.app.activityViewModels

internal class PaymentOptionsListFragment : BasePaymentMethodsListFragment(
    canClickSelectedItem = true
) {
    private val activityViewModel by activityViewModels<PaymentOptionsViewModel> {
        PaymentOptionsViewModel.Factory {
            requireNotNull(
                requireArguments().getParcelable(PaymentOptionsActivity.EXTRA_STARTER_ARGS)
            )
        }
    }

    override val sheetViewModel: PaymentOptionsViewModel by lazy { activityViewModel }

    override fun onPaymentOptionsItemSelected(item: PaymentOptionsItem) {
        super.onPaymentOptionsItemSelected(item)
        sheetViewModel.onUserSelection()
    }
}
