package com.stripe.android.paymentsheet

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import com.stripe.android.R

internal class PaymentOptionsListFragment : BasePaymentMethodsListFragment() {
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

    // TODO(mshafrir-stripe): implement
    override val shouldShowGooglePay: Boolean = false

    override fun transitionToAddPaymentMethod() {
        activityViewModel.transitionTo(
            PaymentOptionsViewModel.TransitionTarget.AddPaymentMethodFull
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewBinding.header.setText(R.string.stripe_paymentsheet_select_payment_method)
    }
}
