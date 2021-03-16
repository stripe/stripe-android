package com.stripe.android.paymentsheet

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import com.stripe.android.R
import com.stripe.android.databinding.FragmentPaymentsheetAddCardBinding
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.FragmentConfig
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.BaseSheetActivity
import java.util.Currency
import java.util.Locale

internal class PaymentSheetAddCardFragment(
    eventReporter: EventReporter
) : BaseAddCardFragment(eventReporter) {
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
        val googlePayButton = viewBinding.googlePayButton
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
                sheetViewModel.checkout()
            }
        }
    }

    override fun createHeaderText(
        config: FragmentConfig
    ): String {
        val amount = config.paymentIntent.amount
        val currencyCode = config.paymentIntent.currency
        return if (amount != null && currencyCode != null) {
            val currency = Currency.getInstance(
                currencyCode.toUpperCase(Locale.ROOT)
            )

            resources.getString(
                R.string.stripe_paymentsheet_pay_using_with_amount,
                CurrencyFormatter().format(amount, currency)
            )
        } else {
            resources.getString(R.string.stripe_paymentsheet_pay_using)
        }
    }
}
