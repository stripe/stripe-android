package com.stripe.android.paymentsheet

import android.os.Bundle
import android.view.View
import androidx.core.view.isInvisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.stripe.android.R
import com.stripe.android.databinding.FragmentPaymentSheetLoadingBinding

/**
 * A `Fragment` that shows a progress indicator.
 */
internal class PaymentSheetLoadingFragment : Fragment(R.layout.fragment_payment_sheet_loading) {

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

        val loadingSpinner = FragmentPaymentSheetLoadingBinding.bind(view).loadingSpinner
        activityViewModel.transition.observe(viewLifecycleOwner) { target ->
            if (target != null) {
                loadingSpinner.isInvisible = true
            }
        }
    }
}
