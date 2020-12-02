package com.stripe.android.paymentsheet

import android.os.Bundle
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import com.stripe.android.paymentsheet.model.AddPaymentMethodConfig
import com.stripe.android.paymentsheet.model.PaymentSelection

internal class PaymentSheetAddCardFragment : BaseAddCardFragment() {
    override val sheetViewModel by activityViewModels<PaymentSheetViewModel> {
        PaymentSheetViewModel.Factory(
            { requireActivity().application },
            {
                requireNotNull(
                    requireArguments().getParcelable(PaymentSheetActivity.EXTRA_STARTER_ARGS)
                )
            }
        )
    }

    @VisibleForTesting
    internal val googlePayButton: View by lazy { viewBinding.googlePayButton }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sheetViewModel.fetchAddPaymentMethodConfig().observe(viewLifecycleOwner) { config ->
            if (config != null) {
                onConfigReady(config)
            }
        }
    }

    @VisibleForTesting
    fun onConfigReady(config: AddPaymentMethodConfig) {
        val shouldShowGooglePayButton = config.shouldShowGooglePayButton
        googlePayButton.setOnClickListener {
            launchGooglePay()
        }
        googlePayButton.isVisible = shouldShowGooglePayButton
        viewBinding.googlePayDivider.isVisible = shouldShowGooglePayButton
        viewBinding.addCardHeader.isVisible = !shouldShowGooglePayButton
    }

    private fun launchGooglePay() {
        sheetViewModel.updateSelection(PaymentSelection.GooglePay)
        sheetViewModel.checkout(requireActivity())
    }
}
