package com.stripe.android.paymentsheet

import androidx.fragment.app.activityViewModels
import com.stripe.android.R
import com.stripe.android.paymentsheet.analytics.EventReporter
import java.util.Currency
import java.util.Locale

internal class PaymentSheetPaymentMethodsListFragment(
    eventReporter: EventReporter
) : BasePaymentMethodsListFragment(
    canClickSelectedItem = false,
    eventReporter
) {
    private val activityViewModel by activityViewModels<PaymentSheetViewModel> {
        PaymentSheetViewModel.Factory(
            { requireActivity().application },
            {
                requireNotNull(
                    requireArguments().getParcelable(PaymentSheetActivity.EXTRA_STARTER_ARGS)
                )
            }
        )
    }

    override val sheetViewModel: PaymentSheetViewModel by lazy { activityViewModel }

    private val currencyFormatter = CurrencyFormatter()

    override fun transitionToAddPaymentMethod() {
        activityViewModel.transitionTo(
            PaymentSheetViewModel.TransitionTarget.AddPaymentMethodFull(config)
        )
    }

    override fun createHeaderText(): String {
        val paymentIntent = config.paymentIntent
        return if (paymentIntent.currency != null && paymentIntent.amount != null) {
            val currency = Currency.getInstance(paymentIntent.currency.toUpperCase(Locale.ROOT))
            getString(
                R.string.stripe_paymentsheet_pay_using_with_amount,
                currencyFormatter.format(paymentIntent.amount, currency)
            )
        } else {
            getString(R.string.stripe_paymentsheet_pay_using)
        }
    }
}
