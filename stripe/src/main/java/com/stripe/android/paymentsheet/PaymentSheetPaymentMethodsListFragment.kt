package com.stripe.android.paymentsheet

import android.app.Application
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.AndroidViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import com.stripe.android.R
import com.stripe.android.databinding.FragmentPaymentsheetPaymentMethodsListBinding
import com.stripe.android.paymentsheet.model.Selection

internal class PaymentSheetPaymentMethodsListFragment : Fragment(R.layout.fragment_paymentsheet_payment_methods_list) {
    private val activityViewModel by activityViewModels<PaymentSheetViewModel> {
        PaymentSheetViewModel.Factory(requireActivity().application)
    }

    private val fragmentViewModel by viewModels<ViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (activity == null) {
            return
        }

        val binding = FragmentPaymentsheetPaymentMethodsListBinding.bind(view)
        binding.recycler.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        activityViewModel.paymentMethods.observe(viewLifecycleOwner) { paymentMethods ->
            binding.recycler.adapter = PaymentSheetPaymentMethodsAdapter(
                paymentMethods,
                fragmentViewModel.selectedPaymentMethod,
                paymentMethodSelectedListener = {
                    fragmentViewModel.selectedPaymentMethod = it
                },
                addCardClickListener = {
                    activityViewModel.transitionTo(PaymentSheetViewModel.TransitionTarget.AddCard)
                }
            )
        }

        // Only fetch the payment methods list if we haven't already
        if (activityViewModel.paymentMethods.value == null) {
            activityViewModel.updatePaymentMethods(requireActivity().intent)
        }
    }

    internal class ViewModel(application: Application) : AndroidViewModel(application) {
        internal var selectedPaymentMethod: Selection? = null
    }
}
