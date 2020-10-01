package com.stripe.android.paymentsheet

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.stripe.android.R

internal class PaymentSheetAddCardFragment : Fragment(R.layout.fragment_paymentsheet_add_card) {
    private val activityViewModel by activityViewModels<PaymentSheetViewModel> {
        PaymentSheetViewModel.Factory(requireActivity().application)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (activity == null) {
            return
        }

        activityViewModel.setSelection(null)
    }
}
