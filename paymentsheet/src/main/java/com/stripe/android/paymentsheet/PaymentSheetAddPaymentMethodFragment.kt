package com.stripe.android.paymentsheet

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.paymentsheet.PaymentSheetViewModel.CheckoutIdentifier
import com.stripe.android.paymentsheet.databinding.FragmentPaymentsheetAddPaymentMethodBinding
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.PaymentSheetViewState
import com.stripe.android.paymentsheet.ui.BaseSheetActivity
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.ui.core.PaymentsThemeConfig
import com.stripe.android.ui.core.createTextSpanFromTextStyle
import com.stripe.android.ui.core.isSystemDarkTheme

internal class PaymentSheetAddPaymentMethodFragment() : BaseAddPaymentMethodFragment() {
    override val viewModelFactory: ViewModelProvider.Factory = PaymentSheetViewModel.Factory(
        { requireActivity().application },
        {
            requireNotNull(
                requireArguments().getParcelable(PaymentSheetActivity.EXTRA_STARTER_ARGS)
            )
        },
        (activity as? AppCompatActivity) ?: this,
        (activity as? AppCompatActivity)?.intent?.extras
    )

    override val sheetViewModel by activityViewModels<PaymentSheetViewModel> {
        viewModelFactory
    }

    private lateinit var viewBinding: FragmentPaymentsheetAddPaymentMethodBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val config = arguments?.getParcelable<com.stripe.android.paymentsheet.model.FragmentConfig>(
            BaseSheetActivity.EXTRA_FRAGMENT_CONFIG
        )
        val shouldShowGooglePayButton = config?.let {
            config.isGooglePayReady && sheetViewModel.paymentMethods.value.isNullOrEmpty()
        } ?: false

        viewBinding = FragmentPaymentsheetAddPaymentMethodBinding.bind(view)
        val googlePayButton = viewBinding.googlePayButton

        googlePayButton.setOnClickListener {
            // The scroll will be made visible onResume of the activity
            sheetViewModel.setContentVisible(false)
            sheetViewModel.lastSelectedPaymentMethod = sheetViewModel.selection.value
            sheetViewModel.updateSelection(PaymentSelection.GooglePay)
        }

        googlePayButton.isVisible = shouldShowGooglePayButton
        sheetViewModel.googlePayDividerVisibilility.postValue(shouldShowGooglePayButton)
        sheetViewModel.headerVisibilility.postValue(!shouldShowGooglePayButton)

        sheetViewModel.selection.observe(viewLifecycleOwner) { paymentSelection ->
            updateErrorMessage(null)
            if (paymentSelection == PaymentSelection.GooglePay) {
                sheetViewModel.checkout(CheckoutIdentifier.AddFragmentTopGooglePay)
            }
        }

        sheetViewModel.getButtonStateObservable(CheckoutIdentifier.AddFragmentTopGooglePay)
            .observe(viewLifecycleOwner) { viewState ->
                if (viewState is PaymentSheetViewState.Reset) {
                    // If Google Pay was cancelled or failed, re-select the form payment method
                    sheetViewModel.updateSelection(sheetViewModel.lastSelectedPaymentMethod)
                }

                updateErrorMessage(viewState?.errorMessage)
                googlePayButton.updateState(viewState?.convert())
            }

        sheetViewModel.processing.observe(viewLifecycleOwner) { isProcessing ->
            googlePayButton.isEnabled = !isProcessing
        }
    }

    private fun updateErrorMessage(userMessage: BaseSheetViewModel.UserErrorMessage?) {
        userMessage?.message.let { message ->
            context?.let {
                viewBinding.message.text = createTextSpanFromTextStyle(
                    text = message,
                    context = it,
                    textStyle = PaymentsThemeConfig.Typography.h6,
                    color = PaymentsThemeConfig.colors(it.isSystemDarkTheme()).error,
                    fontFamily = PaymentsThemeConfig.Typography.fontFamily
                )
            }
        }
        viewBinding.message.isVisible = userMessage != null
    }
}
