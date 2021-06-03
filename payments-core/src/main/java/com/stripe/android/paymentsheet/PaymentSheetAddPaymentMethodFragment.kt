package com.stripe.android.paymentsheet

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.R
import com.stripe.android.databinding.FragmentPaymentsheetAddPaymentMethodBinding
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.FragmentConfig
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.PaymentSheetViewState
import com.stripe.android.paymentsheet.ui.BaseSheetActivity

internal class PaymentSheetAddPaymentMethodFragment(
    eventReporter: EventReporter
) : BaseAddPaymentMethodFragment(eventReporter) {
    override val viewModelFactory: ViewModelProvider.Factory = PaymentSheetViewModel.Factory(
        { requireActivity().application },
        {
            requireNotNull(
                requireArguments().getParcelable(PaymentSheetActivity.EXTRA_STARTER_ARGS)
            )
        }
    )

    override val sheetViewModel by activityViewModels<PaymentSheetViewModel> {
        viewModelFactory
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val config = arguments?.getParcelable<FragmentConfig>(
            BaseSheetActivity.EXTRA_FRAGMENT_CONFIG
        )
        val shouldShowGooglePayButton = config?.let {
            config.isGooglePayReady && config.paymentMethods.isEmpty()
        } ?: false

        val viewBinding = FragmentPaymentsheetAddPaymentMethodBinding.bind(view)
        val googlePayButton = viewBinding.googlePayButton
        val messageView = viewBinding.message
        val googlePayDivider = viewBinding.googlePayDivider

        googlePayButton.setOnClickListener {
            sheetViewModel.updateSelection(PaymentSelection.GooglePay)
        }

        googlePayButton.isVisible = shouldShowGooglePayButton
        googlePayDivider.isVisible = shouldShowGooglePayButton
        addPaymentMethodHeader.isVisible = !shouldShowGooglePayButton

        sheetViewModel.selection.observe(viewLifecycleOwner) { paymentSelection ->
            if (paymentSelection == PaymentSelection.GooglePay) {
                sheetViewModel.checkout(PaymentSheetViewModel.CheckoutIdentifier.AddFragmentTopGooglePay)
            }
        }

        sheetViewModel.getButtonStateObservable(PaymentSheetViewModel.CheckoutIdentifier.AddFragmentTopGooglePay)
            .observe(viewLifecycleOwner) { viewState ->
                messageView.isVisible = viewState?.errorMessage != null
                messageView.text = viewState?.errorMessage?.message
                googlePayButton.updateState(viewState?.convert())

                if (viewState is PaymentSheetViewState.Reset) {
                    updateSelection()
                }
            }

        sheetViewModel.processing.observe(viewLifecycleOwner) { isProcessing ->
            googlePayButton.isEnabled = !isProcessing
        }
    }

    private fun updateSelection() {
        // TODO(brnunes-stripe): Fix this
        (
            childFragmentManager
                .findFragmentById(R.id.payment_method_fragment_container) as? AddCardFragment<*>
            )?.updateSelection()
    }
}
