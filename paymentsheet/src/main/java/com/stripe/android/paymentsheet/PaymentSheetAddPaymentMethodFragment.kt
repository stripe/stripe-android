package com.stripe.android.paymentsheet

import androidx.fragment.app.activityViewModels
import kotlinx.coroutines.FlowPreview

@OptIn(FlowPreview::class)
internal class PaymentSheetAddPaymentMethodFragment : BaseAddPaymentMethodFragment() {

    override val sheetViewModel by activityViewModels<PaymentSheetViewModel> {
        PaymentSheetViewModel.Factory {
            requireNotNull(requireArguments().getParcelable(PaymentSheetActivity.EXTRA_STARTER_ARGS))
        }
    }
}
