package com.stripe.android.paymentsheet

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import com.stripe.android.R
import com.stripe.android.databinding.FragmentPaymentsheetPaymentMethodsListBinding
import com.stripe.android.paymentsheet.PaymentSheetViewModel.SheetMode
import com.stripe.android.paymentsheet.model.PaymentSelection

internal class PaymentSheetPaymentMethodsListFragment : Fragment(R.layout.fragment_paymentsheet_payment_methods_list) {
    private val activityViewModel by activityViewModels<PaymentSheetViewModel> {
        PaymentSheetViewModel.Factory {
            requireActivity().application
        }
    }

    private val fragmentViewModel by viewModels<VM>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (activity == null) {
            return
        }

        // If we're returning to this fragment from elsewhere, we need to reset the selection to whatever
        // the user had selected previously
        activityViewModel.updateSelection(fragmentViewModel.selectedPaymentMethod)
        // reset the mode in case we're returning from the back stack
        activityViewModel.updateMode(SheetMode.WRAPPED)

        val binding = FragmentPaymentsheetPaymentMethodsListBinding.bind(view)
        binding.recycler.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        activityViewModel.paymentMethods.observe(viewLifecycleOwner) { paymentMethods ->
            binding.recycler.adapter = PaymentSheetPaymentMethodsAdapter(
                paymentMethods,
                fragmentViewModel.selectedPaymentMethod,
                paymentMethodSelectedListener = {
                    fragmentViewModel.selectedPaymentMethod = it
                    activityViewModel.updateSelection(it)
                },
                addCardClickListener = {
                    activityViewModel.transitionTo(PaymentSheetViewModel.TransitionTarget.AddPaymentMethodFull)
                }
            )
        }

        // Only fetch the payment methods list if we haven't already
        if (activityViewModel.paymentMethods.value == null) {
            activityViewModel.updatePaymentMethods(requireActivity().intent)
        }
    }

    internal class VM : ViewModel() {
        internal var selectedPaymentMethod: PaymentSelection? = null
    }
}
