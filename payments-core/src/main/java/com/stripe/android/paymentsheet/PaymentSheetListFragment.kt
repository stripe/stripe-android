package com.stripe.android.paymentsheet

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import com.stripe.android.R
import com.stripe.android.databinding.FragmentPaymentsheetPaymentMethodsListBinding
import com.stripe.android.paymentsheet.analytics.EventReporter

internal class PaymentSheetListFragment(
    eventReporter: EventReporter
) : BasePaymentMethodsListFragment(
    canClickSelectedItem = false,
    eventReporter
) {
    private val currencyFormatter = CurrencyFormatter()
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

    override val sheetViewModel: PaymentSheetViewModel by lazy { activityViewModel }

    override fun transitionToAddPaymentMethod() {
        activityViewModel.transitionTo(
            PaymentSheetViewModel.TransitionTarget.AddPaymentMethodFull(config)
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewBinding = FragmentPaymentsheetPaymentMethodsListBinding.bind(view)

        if (sheetViewModel.isProcessingPaymentIntent) {
            sheetViewModel.amount.observe(viewLifecycleOwner) {
                viewBinding.total.text = getTotalText(it)
            }
        } else {
            viewBinding.total.isVisible = false
        }
    }

    private fun getTotalText(amount: PaymentSheetViewModel.Amount): String {
        return resources.getString(
            R.string.stripe_paymentsheet_total_amount,
            currencyFormatter.format(amount.value, amount.currencyCode)
        )
    }
}
