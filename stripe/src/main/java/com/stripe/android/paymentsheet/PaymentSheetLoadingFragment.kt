package com.stripe.android.paymentsheet

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.stripe.android.R
import com.stripe.android.paymentsheet.PaymentSheetViewModel.TransitionTarget

class PaymentSheetLoadingFragment : Fragment(R.layout.fragment_payment_sheet_loading) {
    private val activityViewModel by activityViewModels<PaymentSheetViewModel> {
        PaymentSheetViewModel.Factory {
            requireActivity().application
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (activity == null) {
            return
        }

        activityViewModel.paymentMethods.observe(requireActivity()) { paymentMethods ->
            val target = if (paymentMethods.isEmpty()) {
                TransitionTarget.AddPaymentMethodSheet
            } else {
                TransitionTarget.SelectSavedPaymentMethod
            }
            activityViewModel.transitionTo(target)
        }
        activityViewModel.updatePaymentMethods(requireActivity().intent)

        // TODO: Fetch payment intent before transitioning
    }
}
