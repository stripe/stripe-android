package com.stripe.android.paymentsheet

import androidx.fragment.app.activityViewModels
import kotlinx.coroutines.FlowPreview

@FlowPreview
internal class PaymentOptionsAddPaymentMethodFragment : BaseAddPaymentMethodFragment() {
    override val sheetViewModel by activityViewModels<PaymentOptionsViewModel> {
        PaymentOptionsViewModel.Factory {
            requireNotNull(PaymentOptionContract.Args.fromIntent(requireActivity().intent))
        }
    }
}
