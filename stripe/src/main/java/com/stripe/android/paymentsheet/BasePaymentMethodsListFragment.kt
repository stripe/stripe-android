package com.stripe.android.paymentsheet

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import com.stripe.android.R
import com.stripe.android.databinding.FragmentPaymentsheetPaymentMethodsListBinding
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.ui.SheetMode
import com.stripe.android.paymentsheet.viewmodels.SheetViewModel

internal abstract class BasePaymentMethodsListFragment(
    private val canClickSelectedItem: Boolean,
    private val eventReporter: EventReporter
) : Fragment(
    R.layout.fragment_paymentsheet_payment_methods_list
) {
    abstract val sheetViewModel: SheetViewModel<*>

    private val fragmentViewModel by viewModels<PaymentMethodsViewModel>()

    protected val adapter: PaymentOptionsAdapter by lazy {
        PaymentOptionsAdapter(
            canClickSelectedItem,
            paymentOptionSelectedListener = ::onPaymentOptionSelected,
            addCardClickListener = {
                transitionToAddPaymentMethod()
            }
        ).also {
            it.paymentSelection = fragmentViewModel.currentPaymentSelection
        }
    }

    private var _viewBinding: FragmentPaymentsheetPaymentMethodsListBinding? = null
    protected val viewBinding get() = requireNotNull(_viewBinding)

    abstract fun transitionToAddPaymentMethod()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // If we're returning to this fragment from elsewhere, we need to reset the selection to
        // whatever the user had selected previously
        sheetViewModel.updateSelection(fragmentViewModel.currentPaymentSelection)
        // reset the mode in case we're returning from the back stack
        sheetViewModel.updateMode(SheetMode.Wrapped)

        _viewBinding = FragmentPaymentsheetPaymentMethodsListBinding.bind(view)
        viewBinding.recycler.layoutManager = LinearLayoutManager(
            activity,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        viewBinding.recycler.adapter = adapter

        sheetViewModel.getSavedSelection()
            .observe(viewLifecycleOwner) { savedSelection ->
                when (savedSelection) {
                    SavedSelection.GooglePay -> {
                        adapter.paymentSelection = PaymentSelection.GooglePay
                    }
                    is SavedSelection.PaymentMethod -> {
                        adapter.defaultPaymentMethodId = savedSelection.id
                    }
                    else -> {
                        // noop
                    }
                }
            }

        sheetViewModel.paymentMethods.observe(viewLifecycleOwner) { paymentMethods ->
            adapter.paymentMethods = paymentMethods
        }

        sheetViewModel.isGooglePayReady.observe(viewLifecycleOwner) { isGooglePayReady ->
            adapter.shouldShowGooglePay = isGooglePayReady
        }

        eventReporter.onShowExistingPaymentOptions()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _viewBinding = null
    }

    open fun onPaymentOptionSelected(
        paymentSelection: PaymentSelection,
        isClick: Boolean
    ) {
        fragmentViewModel.currentPaymentSelection = paymentSelection
        sheetViewModel.updateSelection(paymentSelection)
    }

    internal class PaymentMethodsViewModel : ViewModel() {
        internal var currentPaymentSelection: PaymentSelection? = null
    }
}
