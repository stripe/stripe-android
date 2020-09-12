package com.stripe.android.checkout

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.stripe.android.R
import com.stripe.android.databinding.FragmentCheckoutPaymentMethodsListBinding

internal class CheckoutPaymentMethodsListFragment : Fragment(R.layout.fragment_checkout_payment_methods_list) {
    private val viewModel by activityViewModels<CheckoutViewModel> {
        CheckoutViewModel.Factory(requireActivity().application)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentCheckoutPaymentMethodsListBinding.bind(view)
        binding.recycler.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        viewModel.getPaymentMethods(requireActivity()).observe(viewLifecycleOwner) {
            binding.recycler.adapter = CheckoutPaymentMethodsAdapter(it) {
                viewModel.transitionTo(CheckoutViewModel.TransitionTarget.AddCard)
            }
        }
    }
}
