package com.stripe.android.paymentsheet

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import kotlinx.coroutines.FlowPreview

@OptIn(FlowPreview::class)
internal class PaymentSheetAddPaymentMethodFragment() : BaseAddPaymentMethodFragment() {

    override val sheetViewModel by activityViewModels<PaymentSheetViewModel> {
        PaymentSheetViewModel.Factory {
            requireNotNull(requireArguments().getParcelable(PaymentSheetActivity.EXTRA_STARTER_ARGS))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sheetViewModel.showTopContainer.observe(viewLifecycleOwner) { visible ->
            sheetViewModel.headerText.value = if (visible) {
                null
            } else {
                getString(R.string.stripe_paymentsheet_add_payment_method_title)
            }
        }
    }
}
