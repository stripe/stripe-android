package com.stripe.android.paymentsheet

import android.view.View
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import com.stripe.android.R
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.FragmentConfig
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

    internal val header: TextView by lazy { viewBinding.header }

    override fun transitionToAddPaymentMethod() {
        activityViewModel.transitionTo(
            PaymentSheetViewModel.TransitionTarget.AddPaymentMethodFull(config)
        )
    }

    override fun onPostViewCreated(
        view: View,
        fragmentConfig: FragmentConfig
    ) {
        super.onPostViewCreated(view, fragmentConfig)

        val paymentIntent = fragmentConfig.paymentIntent
        if (paymentIntent.currency != null && paymentIntent.amount != null) {
            updateHeader(paymentIntent.amount, paymentIntent.currency)
        } else {
            viewBinding.header.setText(R.string.stripe_paymentsheet_pay_using)
        }
    }

    private fun updateHeader(
        amount: Long,
        currencyCode: String
    ) {
        val currency = Currency.getInstance(currencyCode.toUpperCase(Locale.ROOT))
        header.text = getString(
            R.string.stripe_paymentsheet_pay_using_with_amount,
            currencyFormatter.format(amount, currency)
        )
    }
}
