package com.stripe.android.paymentsheet

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import com.stripe.android.databinding.FragmentPaymentsheetAddCardBinding
import com.stripe.android.databinding.StripeGooglePayButtonBinding
import com.stripe.android.databinding.StripeGooglePayButtonOverlayBinding
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.FragmentConfig
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.ViewState
import com.stripe.android.paymentsheet.ui.BaseSheetActivity
import com.stripe.android.paymentsheet.ui.GooglePayButton

internal class PaymentSheetAddCardFragment(
    eventReporter: EventReporter
) : BaseAddCardFragment(eventReporter) {
    private lateinit var googlePayButton: GooglePayButton
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val config = arguments?.getParcelable<FragmentConfig>(
            BaseSheetActivity.EXTRA_FRAGMENT_CONFIG
        )
        val shouldShowGooglePayButton = config?.let {
            config.isGooglePayReady && config.paymentMethods.isEmpty()
        } ?: false

        val viewBinding = FragmentPaymentsheetAddCardBinding.bind(view)
        googlePayButton = viewBinding.googlePayButton
        val addCardHeader = viewBinding.addCardHeader
        val googlePayDivider = viewBinding.googlePayDivider

        googlePayButton.setOnClickListener {
            sheetViewModel.updateSelection(PaymentSelection.GooglePay)
//        googlePayButton.isEnabled = false
            googlePayButton.updateState(GooglePayButton.State.StartProcessing)
//        googlePayButton.updateState(GooglePayButton.State.FinishProcessing({
//
//        }))
        }

        googlePayButton.isVisible = shouldShowGooglePayButton
        googlePayButton.updateState(GooglePayButton.State.Ready(""))
        googlePayButton.isEnabled = true

        val googlePayButtonDetails = StripeGooglePayButtonBinding.bind(googlePayButton)
        googlePayButton.addView(
            StripeGooglePayButtonOverlayBinding.inflate(
                layoutInflater,
                googlePayButton.viewBinding.customerSupplied,
                false
            ).root
        )

        googlePayDivider.isVisible = shouldShowGooglePayButton
        addCardHeader.isVisible = !shouldShowGooglePayButton

        sheetViewModel.selection.observe(viewLifecycleOwner) { paymentSelection ->
            if (paymentSelection == PaymentSelection.GooglePay) {
                sheetViewModel.checkout()
            }
        }

        sheetViewModel.viewState.observe(viewLifecycleOwner) { viewState ->
            if (viewState is ViewState.PaymentSheet.Ready) {
                updateSelection()
            }
        }
    }
}
