package com.stripe.android.paymentsheet

import androidx.fragment.app.activityViewModels
import com.stripe.android.paymentsheet.state.GooglePayState

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

    override fun onResume() {
        super.onResume()

        sheetViewModel.updateHeaderText(
            getString(
                if (
                    sheetViewModel.isLinkEnabled.value == true ||
                    sheetViewModel.googlePayState.value == GooglePayState.Available
                ) {
                    R.string.stripe_paymentsheet_pay_using
                } else {
                    R.string.stripe_paymentsheet_select_payment_method
                }
            )
        )
    }
}
