package com.stripe.android.view

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.stripe.android.R
import com.stripe.android.databinding.FragmentCheckoutPaymentMethodsListBinding
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod

internal class CheckoutPaymentMethodsListFragment : Fragment(R.layout.fragment_checkout_payment_methods_list) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentCheckoutPaymentMethodsListBinding.bind(view)
        binding.recycler.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        // TODO: Use real data
        binding.recycler.adapter = CheckoutPaymentMethodsAdapter(
            listOf(
                PaymentMethod(
                    "amex",
                    0,
                    false,
                    PaymentMethod.Type.Card,
                    card = PaymentMethod.Card(CardBrand.AmericanExpress, last4 = "1234")
                ),
                PaymentMethod(
                    "visa",
                    0,
                    false,
                    PaymentMethod.Type.Card,
                    card = PaymentMethod.Card(CardBrand.Visa, last4 = "4242")
                ),
                PaymentMethod(
                    "mastercard",
                    0,
                    false,
                    PaymentMethod.Type.Card,
                    card = PaymentMethod.Card(CardBrand.MasterCard, last4 = "6789")
                )
            )
        )
    }
}
