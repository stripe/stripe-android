package com.stripe.android.paymentsheet.paymentdatacollection

import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.stripe.android.paymentsheet.viewmodels.PaymentDataCollectionViewModel

internal abstract class BasePaymentDataCollectionFragment : Fragment() {
    protected val viewModel: PaymentDataCollectionViewModel by viewModels {
        PaymentDataCollectionViewModel.Factory { requireActivity().application }
    }
}
