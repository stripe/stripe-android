package com.stripe.android.paymentsheet

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import com.stripe.android.paymentsheet.databinding.FragmentPaymentsheetPaymentMethodsListBinding
import com.stripe.android.ui.core.CurrencyFormatter
import com.stripe.android.ui.core.elements.H4Text
import com.stripe.android.ui.core.elements.H6Text

internal class PaymentSheetListFragment() : BasePaymentMethodsListFragment(
    canClickSelectedItem = false
) {
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
        viewBinding.header.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                HeaderUI(
                    viewModel = activityViewModel,
                    totalVisible = sheetViewModel.isProcessingPaymentIntent
                )
            }
        }
    }
}

@Composable
private fun HeaderUI(
    viewModel: PaymentSheetViewModel,
    totalVisible: Boolean
) {
    val amount = remember { viewModel.amount }
    Column {
        H4Text(
            text = stringResource(R.string.stripe_paymentsheet_select_payment_method),
            modifier = Modifier.padding(bottom = 2.dp)
        )
        if (totalVisible) {
            amount.value?.let {
                H6Text(
                    text = stringResource(
                        R.string.stripe_paymentsheet_total_amount,
                        CurrencyFormatter.format(it.value, it.currencyCode)
                    )
                )
            }
        }
    }
}
