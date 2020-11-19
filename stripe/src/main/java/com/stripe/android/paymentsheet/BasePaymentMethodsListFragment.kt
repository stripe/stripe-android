package com.stripe.android.paymentsheet

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import com.stripe.android.R
import com.stripe.android.databinding.FragmentPaymentsheetPaymentMethodsListBinding
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.SheetMode
import com.stripe.android.paymentsheet.viewmodels.SheetViewModel

internal abstract class BasePaymentMethodsListFragment : Fragment(
    R.layout.fragment_paymentsheet_payment_methods_list
) {
    abstract val sheetViewModel: SheetViewModel<*, *>

    private val fragmentViewModel by viewModels<PaymentMethodsViewModel>()

    protected val adapter: PaymentMethodsAdapter by lazy {
        PaymentMethodsAdapter(
            fragmentViewModel.selectedPaymentMethod,
            paymentMethodSelectedListener = {
                fragmentViewModel.selectedPaymentMethod = it
                sheetViewModel.updateSelection(it)
            },
            addCardClickListener = {
                transitionToAddPaymentMethod()
            }
        )
    }

    abstract fun transitionToAddPaymentMethod()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // If we're returning to this fragment from elsewhere, we need to reset the selection to
        // whatever the user had selected previously
        sheetViewModel.updateSelection(fragmentViewModel.selectedPaymentMethod)
        // reset the mode in case we're returning from the back stack
        sheetViewModel.updateMode(SheetMode.Wrapped)

        val binding = FragmentPaymentsheetPaymentMethodsListBinding.bind(view)
        binding.recycler.layoutManager = LinearLayoutManager(
            activity,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        binding.recycler.adapter = adapter
    }

    internal class PaymentMethodsViewModel : ViewModel() {
        internal var selectedPaymentMethod: PaymentSelection? = null
    }
}
