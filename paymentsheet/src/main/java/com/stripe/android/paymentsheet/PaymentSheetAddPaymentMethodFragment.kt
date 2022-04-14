package com.stripe.android.paymentsheet

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider

internal class PaymentSheetAddPaymentMethodFragment() : BaseAddPaymentMethodFragment() {
    override val viewModelFactory: ViewModelProvider.Factory = PaymentSheetViewModel.Factory(
        { requireActivity().application },
        {
            requireNotNull(
                requireArguments().getParcelable(PaymentSheetActivity.EXTRA_STARTER_ARGS)
            )
        },
        (activity as? AppCompatActivity) ?: this,
        (activity as? AppCompatActivity)?.intent?.extras
    )

    override val sheetViewModel by activityViewModels<PaymentSheetViewModel> {
        viewModelFactory
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sheetViewModel.showTopContainer.observe(viewLifecycleOwner) { visible ->
            sheetViewModel.headerText.value = if (visible) null else
                getString(R.string.stripe_paymentsheet_add_payment_method_title)
        }
    }
}