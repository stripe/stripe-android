package com.stripe.android.paymentsheet

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.stripe.android.R
import com.stripe.android.paymentsheet.PaymentSheetViewModel.TransitionTarget

class PaymentSheetLoadingFragment : Fragment(R.layout.fragment_payment_sheet_loading) {
    private val activityViewModel by activityViewModels<PaymentSheetViewModel> {
        PaymentSheetViewModel.Factory(
            { requireActivity().application },
            {
                requireNotNull(
                    requireArguments().getParcelable(PaymentSheetActivity.EXTRA_STARTER_ARGS)
                )
            }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activityViewModel.paymentMethods.observe(viewLifecycleOwner) { paymentMethods ->
            val target = if (paymentMethods.isEmpty()) {
                TransitionTarget.AddPaymentMethodSheet
            } else {
                TransitionTarget.SelectSavedPaymentMethod
            }
            activityViewModel.transitionTo(target)
        }
        activityViewModel.updatePaymentMethods()
        activityViewModel.fetchPaymentIntent()
    }
}
