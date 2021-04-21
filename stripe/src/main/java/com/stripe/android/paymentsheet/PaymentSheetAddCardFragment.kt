package com.stripe.android.paymentsheet

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import com.stripe.android.databinding.FragmentPaymentsheetAddCardBinding
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.FragmentConfig
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.ViewState
import com.stripe.android.paymentsheet.ui.BaseSheetActivity
import com.stripe.android.paymentsheet.ui.GooglePayButton
import com.stripe.android.paymentsheet.ui.PrimaryButton

internal class PaymentSheetAddCardFragment(
    eventReporter: EventReporter
) : BaseAddCardFragment(eventReporter) {
    private lateinit var viewBinding: FragmentPaymentsheetAddCardBinding
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

    private val googleViewStateObserver = { viewState: ViewState.PaymentSheet? ->
        when (viewState) {
            is ViewState.PaymentSheet.Ready -> googlePayButton.updateState(
                PrimaryButton.State.Ready()
            )
            is ViewState.PaymentSheet.StartProcessing -> googlePayButton.updateState(
                PrimaryButton.State.StartProcessing
            )
            is ViewState.PaymentSheet.FinishProcessing -> googlePayButton.updateState(
                PrimaryButton.State.FinishProcessing(viewState.onComplete)
            )
            is ViewState.PaymentSheet.ProcessResult -> {
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val config = arguments?.getParcelable<FragmentConfig>(
            BaseSheetActivity.EXTRA_FRAGMENT_CONFIG
        )
        val shouldShowGooglePayButton = config?.let {
            config.isGooglePayReady && config.paymentMethods.isEmpty()
        } ?: false

        viewBinding = FragmentPaymentsheetAddCardBinding.bind(view)
        googlePayButton = viewBinding.googlePayButton
        val addCardHeader = viewBinding.addCardHeader
        val googlePayDivider = viewBinding.googlePayDivider

        googlePayButton.setOnClickListener {
            sheetViewModel.updateSelection(PaymentSelection.GooglePay)
        }

        googlePayButton.isVisible = shouldShowGooglePayButton

        googlePayDivider.isVisible = shouldShowGooglePayButton
        addCardHeader.isVisible = !shouldShowGooglePayButton

        sheetViewModel.selection.observe(viewLifecycleOwner) { paymentSelection ->
            if (paymentSelection == PaymentSelection.GooglePay) {
                sheetViewModel.checkout(CheckoutIdentifier.AddFragmentTopGooglePay)
            }
        }

        sheetViewModel.viewState.observe(viewLifecycleOwner) { viewState ->
            if (viewState is ViewState.PaymentSheet.Ready) {
                updateSelection()
            }
        }

        sheetViewModel.getViewStateObservable(CheckoutIdentifier.AddFragmentTopGooglePay)
            .observe(viewLifecycleOwner, googleViewStateObserver)
    }
}
