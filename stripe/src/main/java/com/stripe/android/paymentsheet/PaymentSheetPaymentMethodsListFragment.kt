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

internal class PaymentSheetPaymentMethodsListFragment : Fragment(
    R.layout.fragment_paymentsheet_payment_methods_list
) {
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

    private val fragmentViewModel by viewModels<PaymentMethodsViewModel>()

    private val adapter: PaymentSheetPaymentMethodsAdapter by lazy {
        PaymentSheetPaymentMethodsAdapter(
            fragmentViewModel.selectedPaymentMethod,
            paymentMethodSelectedListener = {
                fragmentViewModel.selectedPaymentMethod = it
                activityViewModel.updateSelection(it)
            },
            addCardClickListener = {
                activityViewModel.transitionTo(
                    PaymentSheetViewModel.TransitionTarget.AddPaymentMethodFull
                )
            }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // If we're returning to this fragment from elsewhere, we need to reset the selection to
        // whatever the user had selected previously
        activityViewModel.updateSelection(fragmentViewModel.selectedPaymentMethod)
        // reset the mode in case we're returning from the back stack
        activityViewModel.updateMode(SheetMode.Wrapped)

        val binding = FragmentPaymentsheetPaymentMethodsListBinding.bind(view)
        binding.recycler.layoutManager = LinearLayoutManager(
            activity,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        binding.recycler.adapter = adapter

        activityViewModel.paymentMethods.observe(viewLifecycleOwner) { paymentMethods ->
            adapter.paymentMethods = paymentMethods
        }

        // Only fetch the payment methods list if we haven't already
        if (activityViewModel.paymentMethods.value == null) {
            activityViewModel.updatePaymentMethods()
        }
    }

    internal class PaymentMethodsViewModel : ViewModel() {
        internal var selectedPaymentMethod: PaymentSelection? = null
    }
}
