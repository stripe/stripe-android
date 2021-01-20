package com.stripe.android.paymentsheet

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import com.stripe.android.R
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection

internal class PaymentOptionsListFragment(
    eventReporter: EventReporter
) : BasePaymentMethodsListFragment(eventReporter) {
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
            PaymentOptionsViewModel.TransitionTarget.AddPaymentMethodFull
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewBinding.header.setText(R.string.stripe_paymentsheet_select_payment_method)
    }

    override fun onPaymentOptionSelected(
        paymentSelection: PaymentSelection,
        isClick: Boolean
    ) {
        super.onPaymentOptionSelected(paymentSelection, isClick)
        if (isClick) {
            // this is a click-triggered selection
            sheetViewModel.onUserSelection(paymentSelection)
        }
    }
}
