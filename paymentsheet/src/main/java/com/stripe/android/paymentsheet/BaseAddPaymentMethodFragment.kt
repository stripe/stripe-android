package com.stripe.android.paymentsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.stripe.android.paymentsheet.ui.AddPaymentMethod
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.uicore.StripeTheme
import kotlinx.coroutines.FlowPreview

@FlowPreview
internal abstract class BaseAddPaymentMethodFragment : Fragment() {
    abstract val sheetViewModel: BaseSheetViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setContent {
            StripeTheme {
                AddPaymentMethod(sheetViewModel)
            }
        }
    }
}
