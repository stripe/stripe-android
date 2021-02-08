package com.stripe.android.paymentsheet

import androidx.fragment.app.activityViewModels
import com.stripe.android.R
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.FragmentConfig
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

    override fun onGooglePaySelected() {
        sheetViewModel.checkout()
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
