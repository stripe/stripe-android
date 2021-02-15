package com.stripe.android.paymentsheet

import androidx.fragment.app.activityViewModels
import com.stripe.android.R
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection

internal class PaymentOptionsListFragment(
    eventReporter: EventReporter
) : BasePaymentMethodsListFragment(
    canClickSelectedItem = true,
    eventReporter
) {
    private val activityViewModel by activityViewModels<PaymentOptionsViewModel> {
        PaymentOptionsViewModel.Factory(
            { requireActivity().application },
            {
                requireNotNull(
                    requireArguments().getParcelable(PaymentOptionsActivity.EXTRA_STARTER_ARGS)
                )
            }
        )
    }

    override val sheetViewModel: PaymentOptionsViewModel by lazy { activityViewModel }

    override fun transitionToAddPaymentMethod() {
        activityViewModel.transitionTo(
            PaymentOptionsViewModel.TransitionTarget.AddPaymentMethodFull(config)
        )
    }

    override fun createHeaderText() = getString(R.string.stripe_paymentsheet_select_payment_method)

    override fun onPaymentOptionSelected(
        paymentSelection: PaymentSelection,
        isClick: Boolean
    ) {
        super.onPaymentOptionSelected(paymentSelection, isClick)
        if (isClick) {
            // this is a click-triggered selection
            sheetViewModel.onUserSelection()
        }
    }
}
