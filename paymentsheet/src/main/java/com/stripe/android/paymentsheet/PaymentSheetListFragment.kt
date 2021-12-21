package com.stripe.android.paymentsheet

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import com.stripe.android.paymentsheet.databinding.FragmentPaymentsheetPaymentMethodsListBinding
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.CurrencyFormatter

internal class PaymentSheetListFragment(
) : BasePaymentMethodsListFragment(
    canClickSelectedItem = false
) {
    private val currencyFormatter = CurrencyFormatter()
    private val activityViewModel by activityViewModels<PaymentSheetViewModel> {
        PaymentSheetViewModel.Factory(
            { requireActivity().application },
            {
                requireNotNull(
                    requireArguments().getParcelable(PaymentSheetActivity.EXTRA_STARTER_ARGS)
                )
            },
            (activity as? AppCompatActivity) ?: this
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
                viewBinding.total.text = getTotalText(requireNotNull(it))
            }
        } else {
            viewBinding.total.isVisible = false
        }
    }

    private fun getTotalText(amount: Amount): String {
        return resources.getString(
            R.string.stripe_paymentsheet_total_amount,
            currencyFormatter.format(amount.value, amount.currencyCode)
        )
    }
}
