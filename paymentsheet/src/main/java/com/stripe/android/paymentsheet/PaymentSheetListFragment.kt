package com.stripe.android.paymentsheet

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels

internal class PaymentSheetListFragment() : BasePaymentMethodsListFragment(
    canClickSelectedItem = false
) {
    private val activityViewModel by activityViewModels<PaymentSheetViewModel> {
        PaymentSheetViewModel.Factory(
            { requireActivity().application },
            {
                requireNotNull(
                    requireArguments().getParcelable(PaymentSheetActivity.EXTRA_STARTER_ARGS)
                )
            },
            (activity as? AppCompatActivity) ?: this
        )
    }

    override val sheetViewModel: PaymentSheetViewModel by lazy { activityViewModel }

    override fun transitionToAddPaymentMethod() {
        activityViewModel.transitionTo(
            PaymentSheetViewModel.TransitionTarget.AddPaymentMethodFull(config)
        )
    }

    override fun onResume() {
        super.onResume()

        sheetViewModel.headerText.value = getString(
            if (
                sheetViewModel.isLinkEnabled.value == true ||
                sheetViewModel.isGooglePayReady.value == true
            ) {
                R.string.stripe_paymentsheet_pay_using
            } else {
                R.string.stripe_paymentsheet_select_payment_method
            }
        )
    }
}
